import Foundation
import LabsAPI
import SwiftUI

/// The workout endpoints of the backend, for the signed in user.
nonisolated struct WorkoutService: Sendable {
    private let client: APIClient

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
    }

    // MARK: Templates and exercises

    func templates() async throws -> [WorkoutTemplate] {
        try await client.call { try await $0.workoutGetAllWorkouts().ok.body.json }.map(WorkoutTemplate.init)
    }

    func exercises() async throws -> [Exercise] {
        try await client.call { try await $0.workoutGetAllExercises().ok.body.json }.map(Exercise.init)
    }

    // MARK: Sessions

    /// Finished workouts started in the range, newest first.
    func sessions(from: Date, to: Date) async throws -> [WorkoutSession] {
        try await client.call { try await $0.workoutSessionGetSessions(query: .init(from: from, to: to)).ok.body.json }
            .map(WorkoutSession.init)
    }

    /// The running workout, nil when there is none.
    func activeSession() async throws -> WorkoutSession? {
        try await client.call { client in
            switch try await client.workoutSessionGetActive() {
            case .ok(let ok): WorkoutSession(try ok.body.json)
            case .noContent: nil
            case .undocumented(let status, _): throw APIError.server(status: status, message: nil)
            }
        }
    }

    /// Starts a workout from the template, or without exercises when `workoutID` is nil.
    /// Fails with status 400 while another workout is running.
    func start(workoutID: Int?) async throws -> WorkoutSession {
        let body = Components.Schemas.CreateWorkoutSessionDto(workoutId: workoutID.map(Int64.init))
        return WorkoutSession(try await client.call { try await $0.workoutSessionStart(body: .json(body)).ok.body.json })
    }

    /// Saves the current state of a running workout.
    func update(_ sessionID: Int, name: String, exercises: [SessionExercise]) async throws {
        let body = Components.Schemas.UpdateWorkoutSessionDto(exercises: exercises.map(\.dto), name: name)
        _ = try await client.call {
            try await $0.workoutSessionUpdate(path: .init(id: Int64(sessionID)), body: .json(body)).ok
        }
    }

    /// Saves the last state and ends the workout, sets that weren't checked off are dropped.
    func finish(_ sessionID: Int, name: String, exercises: [SessionExercise]) async throws {
        let body = Components.Schemas.UpdateWorkoutSessionDto(exercises: exercises.map(\.dto), name: name)
        _ = try await client.call {
            try await $0.workoutSessionFinish(path: .init(id: Int64(sessionID)), body: .json(body)).ok
        }
    }

    func delete(_ sessionID: Int) async throws {
        _ = try await client.call { try await $0.workoutSessionDelete(path: .init(id: Int64(sessionID))).noContent }
    }
}

extension EnvironmentValues {
    @Entry var workoutService = WorkoutService()
}
