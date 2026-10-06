import Foundation

/// The numbers of the web dashboard, see frontend/app/workouts/stats.ts.
nonisolated enum WorkoutStats {
    /// Weight times reps of the sets, in kg.
    static func volume(_ sets: [SessionSet]) -> Double {
        sets.reduce(0) { $0 + Double($1.reps) * $1.weightKg }
    }

    /// "850 kg", tonnes from 10 t on so it stays short
    static func formattedVolume(_ kg: Double) -> String {
        kg >= 10_000 ? "\((kg / 1000).compactFormatted) t" : "\(kg.roundedFormatted) kg"
    }

    /// The done sets of the exercise in the newest of the sessions (sorted newest first) that contains it.
    static func lastSets(of exerciseId: Int?, in sessions: [WorkoutSession]) -> [SessionSet]? {
        guard let exerciseId else { return nil }
        for session in sessions {
            if let found = session.exercises.first(where: { $0.exerciseId == exerciseId && $0.sets.contains(where: \.done) }) {
                return found.sets.filter(\.done)
            }
        }
        return nil
    }
}

extension TimeInterval {
    /// "45:12" or "1:05:12"
    var workoutDuration: String {
        let duration = Duration.seconds(Int(self))
        return duration.formatted(.time(pattern: self >= 3600 ? .hourMinuteSecond : .minuteSecond))
    }

    /// "1 Std. 5 Min." for the overview
    var workoutHours: String {
        Duration.seconds(Int(self)).formatted(.units(allowed: [.hours, .minutes], width: .abbreviated))
    }
}
