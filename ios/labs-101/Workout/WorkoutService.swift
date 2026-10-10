import Foundation
import SwiftUI

/// The workout endpoints of the backend, for the signed in user.
nonisolated struct WorkoutService: Sendable {
    private let client: APIClient

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
    }

    private var sessionsPath: String {
        "users/me/workout-sessions"
    }

    // MARK: Templates and exercises

    func templates() async throws -> [WorkoutTemplate] {
        try await client.get("workouts")
    }

    func exercises() async throws -> [Exercise] {
        try await client.get("workouts/exercises")
    }

    // MARK: Sessions

    /// Finished workouts started in the range, newest first.
    func sessions(from: Date, to: Date) async throws -> [WorkoutSession] {
        try await client.get(sessionsPath, query: [
            URLQueryItem(name: "from", value: from.ISO8601Format()),
            URLQueryItem(name: "to", value: to.ISO8601Format()),
        ])
    }

    /// The running workout, nil when there is none.
    func activeSession() async throws -> WorkoutSession? {
        try await client.getIfPresent("\(sessionsPath)/active")
    }

    /// Starts a workout from the template, or without exercises when `workoutID` is nil.
    /// Fails with status 400 while another workout is running.
    func start(workoutID: Int?) async throws -> WorkoutSession {
        try await client.send("POST", sessionsPath, body: StartRequest(workoutId: workoutID))
    }

    /// Saves the current state of a running workout.
    func update(_ sessionID: Int, name: String, exercises: [SessionExercise]) async throws {
        let _: WorkoutSession = try await client.send("PUT", "\(sessionsPath)/\(sessionID)",
                                                      body: UpdateRequest(name: name, exercises: exercises))
    }

    /// Saves the last state and ends the workout, sets that weren't checked off are dropped.
    func finish(_ sessionID: Int, name: String, exercises: [SessionExercise]) async throws {
        let _: WorkoutSession = try await client.send("POST", "\(sessionsPath)/\(sessionID)/finish",
                                                      body: UpdateRequest(name: name, exercises: exercises))
    }

    func delete(_ sessionID: Int) async throws {
        try await client.send("DELETE", "\(sessionsPath)/\(sessionID)")
    }

    private struct StartRequest: Encodable {
        let workoutId: Int?
    }

    private struct UpdateRequest: Encodable {
        let name: String
        let exercises: [SessionExercise]
    }
}

extension EnvironmentValues {
    @Entry var workoutService = WorkoutService()
}
