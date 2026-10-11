import Foundation

/// What the iPhone and the Apple Watch send each other through WatchConnectivity.
/// The iPhone owns the workout (it talks to the backend), the watch shows a copy
/// of it and sends actions back. Everything travels as JSON in the `Data` of a key.
nonisolated enum WatchMessage {
    /// iPhone → watch: `WatchWorkoutState`
    static let state = "state"
    /// iPhone → watch: the id of the workout that was finished or discarded, `noWorkout` when none is running
    static let ended = "ended"
    static let noWorkout = -1
    /// watch → iPhone: `WatchAction`, answered with `state` (or `ended`)
    static let action = "action"
    /// watch → iPhone: asks for the exercises that can be added, answered with `catalog`
    static let catalog = "catalog"
    /// iPhone → watch: the action failed, a message for the user
    static let error = "error"

    static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .secondsSince1970
        return encoder
    }()

    static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .secondsSince1970
        return decoder
    }()
}

/// The running workout as the watch sees it.
nonisolated struct WatchWorkoutState: Codable, Equatable, Sendable {
    nonisolated struct Exercise: Codable, Equatable, Identifiable, Sendable {
        let id: UUID
        let name: String
        var sets: [WatchSet]
        /// the done sets of last time, for the hints
        var previous: [WatchSet]?
    }

    nonisolated struct WatchSet: Codable, Equatable, Identifiable, Sendable {
        let id: UUID
        var reps: Int
        var weightKg: Double
        var done: Bool
        /// rest after the set in seconds, 0 turns the timer off
        var rest: Int

        /// "10 × 60 kg", "12 Wdh" without weight
        var formatted: String {
            weightKg > 0 ? "\(reps) × \(weightKg.formatted(.number.precision(.fractionLength(0...2)))) kg" : "\(reps) Wdh"
        }
    }

    let sessionID: Int
    var name: String
    let startedAt: Date
    var exercises: [Exercise]
    /// the running rest, nil when there is none
    var restUntil: Date?
    var restTotal: TimeInterval?

    var allSets: [WatchSet] {
        exercises.flatMap(\.sets)
    }

    var doneSets: [WatchSet] {
        allSets.filter(\.done)
    }

    /// Weight times reps of the done sets, in kg.
    var volume: Double {
        doneSets.reduce(0) { $0 + Double($1.reps) * $1.weightKg }
    }

    /// Applies the action right away on the watch, the answer of the iPhone replaces it shortly after.
    mutating func apply(_ action: WatchAction, now: Date = .now) {
        switch action {
        case .toggle(let exerciseID, let setID):
            guard let (e, s) = index(of: setID, in: exerciseID) else { return }
            exercises[e].sets[s].done.toggle()
            let set = exercises[e].sets[s]
            if set.done && set.rest > 0 {
                restUntil = now.addingTimeInterval(TimeInterval(set.rest))
                restTotal = TimeInterval(set.rest)
            }
        case .update(let exerciseID, let setID, let reps, let weightKg):
            guard let (e, s) = index(of: setID, in: exerciseID) else { return }
            exercises[e].sets[s].reps = max(0, reps)
            exercises[e].sets[s].weightKg = max(0, weightKg)
        case .addSet(let exerciseID):
            guard let e = exercises.firstIndex(where: { $0.id == exerciseID }) else { return }
            let last = exercises[e].sets.last
            exercises[e].sets.append(WatchSet(id: UUID(), reps: last?.reps ?? 10, weightKg: last?.weightKg ?? 0,
                                              done: false, rest: last?.rest ?? 90))
        case .adjustRest(let seconds):
            guard let until = restUntil else { return }
            let left = max(0, until.timeIntervalSince(now) + seconds)
            restUntil = now.addingTimeInterval(left)
            restTotal = max(15, (restTotal ?? left) + seconds)
        case .endRest:
            restUntil = nil
            restTotal = nil
        case .addExercise, .finish, .discard, .refresh:
            // only the iPhone knows the sets of last time and talks to the backend
            break
        }
    }

    private func index(of setID: UUID, in exerciseID: UUID) -> (Int, Int)? {
        guard let e = exercises.firstIndex(where: { $0.id == exerciseID }),
              let s = exercises[e].sets.firstIndex(where: { $0.id == setID }) else { return nil }
        return (e, s)
    }
}

/// A change made on the watch.
nonisolated enum WatchAction: Codable, Equatable, Sendable {
    case toggle(exercise: UUID, set: UUID)
    case update(exercise: UUID, set: UUID, reps: Int, weightKg: Double)
    case addSet(exercise: UUID)
    case addExercise(id: Int)
    case adjustRest(seconds: TimeInterval)
    case endRest
    case finish
    case discard
    /// only asks for the current state
    case refresh
}

/// An exercise the watch can add to the workout.
nonisolated struct WatchCatalogEntry: Codable, Identifiable, Sendable {
    let id: Int
    let name: String
    /// trained in the last weeks, listed first on the watch
    let recent: Bool
}
