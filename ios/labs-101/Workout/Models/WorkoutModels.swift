import Foundation

nonisolated struct Exercise: Codable, Hashable, Identifiable, Sendable {
    nonisolated struct BodyPart: Codable, Hashable, Sendable {
        let slug: String?
    }

    let id: Int
    let name: String
    let bodyParts: [BodyPart]?

    /// The trained muscles in German, every muscle once.
    var muscles: [String] {
        var seen = Set<String>()
        return (bodyParts ?? []).compactMap(\.slug)
            .filter { seen.insert($0).inserted }
            .map { Muscle.name(of: $0) }
    }
}

/// A workout template, see WorkoutController in the backend.
nonisolated struct WorkoutTemplate: Codable, Hashable, Identifiable, Sendable {
    nonisolated struct Item: Codable, Hashable, Sendable {
        let exercise: Exercise
        let sets: [TemplateSet]
    }

    nonisolated struct TemplateSet: Codable, Hashable, Sendable {
        let reps: Int
        let weightKg: Double
    }

    let id: Int
    let name: String
    let workoutExercises: [Item]

    var setCount: Int {
        workoutExercises.reduce(0) { $0 + $1.sets.count }
    }
}

/// A workout of the user, `endedAt` is nil while it is still running.
nonisolated struct WorkoutSession: Codable, Hashable, Identifiable, Sendable {
    let id: Int
    /// the template it was started from, nil for a free workout
    let workoutId: Int?
    var name: String
    let startedAt: Date
    let endedAt: Date?
    var exercises: [SessionExercise]

    var doneSets: [SessionSet] {
        exercises.flatMap { $0.sets.filter(\.done) }
    }

    /// Seconds from the start to the end, or to now while it is running.
    func duration(now: Date = .now) -> TimeInterval {
        max(0, (endedAt ?? now).timeIntervalSince(startedAt))
    }
}

nonisolated struct SessionExercise: Codable, Hashable, Identifiable, Sendable {
    /// only for SwiftUI, not sent to the backend
    var id = UUID()
    let exerciseId: Int?
    /// kept so the history still reads right after the exercise is renamed or deleted
    let name: String
    var sets: [SessionSet]

    enum CodingKeys: String, CodingKey {
        case exerciseId, name, sets
    }

    init(exerciseId: Int?, name: String, sets: [SessionSet]) {
        self.exerciseId = exerciseId
        self.name = name
        self.sets = sets
    }
}

nonisolated struct SessionSet: Codable, Hashable, Identifiable, Sendable {
    /// only for SwiftUI, not sent to the backend
    var id = UUID()
    var reps: Int
    var weightKg: Double
    var rpe: Int?
    var done: Bool
    /// the rest after the set, nil for the default
    var restSeconds: Int?

    enum CodingKeys: String, CodingKey {
        case reps, weightKg, rpe, done, restSeconds
    }

    init(reps: Int, weightKg: Double, rpe: Int? = nil, done: Bool = false, restSeconds: Int? = WorkoutStats.defaultRest) {
        self.reps = reps
        self.weightKg = weightKg
        self.rpe = rpe
        self.done = done
        self.restSeconds = restSeconds
    }

    var rest: Int {
        get { restSeconds ?? WorkoutStats.defaultRest }
        set { restSeconds = newValue }
    }

    /// "10 × 60 kg", "12 Wdh" without weight
    var formatted: String {
        weightKg > 0 ? "\(reps) × \(weightKg.compactFormatted) kg" : "\(reps) Wdh"
    }
}

nonisolated enum Muscle {
    private static let names = [
        "chest": "Brust", "biceps": "Bizeps", "triceps": "Trizeps", "deltoids": "Schultern", "abs": "Bauch",
        "obliques": "Seitlicher Bauch", "quadriceps": "Quadrizeps", "hamstring": "Beinbeuger", "gluteal": "Gesäß",
        "calves": "Waden", "upper-back": "Oberer Rücken", "lower-back": "Unterer Rücken", "trapezius": "Trapez",
        "forearm": "Unterarme", "adductors": "Adduktoren", "tibialis": "Schienbein", "neck": "Nacken",
        "knees": "Knie", "ankles": "Knöchel", "hands": "Hände", "feet": "Füße", "head": "Kopf", "hair": "Kopf",
    ]

    static func name(of slug: String) -> String {
        names[slug] ?? slug
    }
}
