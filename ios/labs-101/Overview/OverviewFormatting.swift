import SwiftUI

extension Double {
    /// seconds -> "7 h 32 min"
    var sleepDuration: String {
        let total = Int((self / 60).rounded())
        let hours = total / 60, minutes = total % 60
        return hours > 0 ? "\(hours) h \(String(format: "%02d", minutes)) min" : "\(minutes) min"
    }

    /// 0.42 -> "42 %"
    func percentFormatted(decimals: Int = 0) -> String {
        formatted(.percent.precision(.fractionLength(decimals)))
    }

    /// 12.345 -> "12,3"
    func decimalFormatted(_ decimals: Int) -> String {
        formatted(.number.precision(.fractionLength(decimals)))
    }

    /// meters -> "5,2"
    func kilometers(decimals: Int = 1) -> String {
        (self / 1000).decimalFormatted(decimals)
    }
}

extension Color {
    // like the web: status colours only for recovery, every other chart shows one measure in the brand blue
    static let recoveryHigh = Color.successBar
    static let recoveryMid = Color.warning
    static let recoveryLow = Color.danger
    static let strain = Color.brand
    static let sleep = Color.brand
    static let heart = Color.brand
    static let hrv = Color.brand
}

// MARK: Status texts, the same as on the web

enum OverviewStatus {
    static func recoveryColor(_ score: Int?) -> Color {
        guard let score else { return .track }
        return score >= 67 ? .recoveryHigh : score >= 34 ? .recoveryMid : .recoveryLow
    }

    static func recoveryGradient(_ score: Int?) -> [Color] {
        let color = recoveryColor(score)
        return [color, color]
    }

    static func recovery(_ score: Int?) -> String {
        guard let score else { return String(localized: "Noch zu wenige Nächte zum Vergleichen") }
        if score >= 67 { return String(localized: "Bereit für hohe Belastung") }
        if score >= 34 { return String(localized: "Moderat belastbar") }
        return String(localized: "Heute auf Erholung achten")
    }

    /// How much strain the recovery allows, like Bevel's strain target.
    static func strainTarget(_ recovery: Int?) -> ClosedRange<Int>? {
        guard let recovery else { return nil }
        if recovery >= 67 { return 55...80 }
        if recovery >= 34 { return 35...60 }
        return 10...35
    }

    static func strain(_ score: Int) -> String {
        if score < 15 { return String(localized: "Ruhetag") }
        if score < 40 { return String(localized: "Leichte Belastung") }
        if score < 65 { return String(localized: "Moderate Belastung") }
        return String(localized: "Hohe Belastung")
    }

    static func sleep(_ sleep: Sleep?) -> String {
        guard let sleep else { return String(localized: "Keine Schlafdaten") }
        if sleep.score >= 85 { return String(localized: "Erholsamer Schlaf") }
        if sleep.score >= 70 { return String(localized: "Guter Schlaf") }
        if sleep.score >= 50 { return String(localized: "Etwas zu wenig Schlaf") }
        return String(localized: "Schlechter Schlaf")
    }
}

// MARK: Workouts

enum WorkoutActivity {
    // HKWorkoutActivityType
    private static let names: [Int: String] = [
        13: "Radfahren", 16: "Crosstrainer", 20: "Funktionelles Krafttraining", 24: "Wandern", 35: "Rudern",
        37: "Laufen", 44: "Treppensteigen", 46: "Schwimmen", 50: "Krafttraining", 52: "Gehen", 57: "Yoga",
        59: "Core-Training", 62: "Beweglichkeit", 63: "HIIT", 73: "Cardio", 80: "Cool-down", 84: "Tauchen",
    ]

    private static let symbols: [Int: String] = [
        13: "figure.outdoor.cycle", 16: "figure.elliptical", 20: "figure.strengthtraining.functional",
        24: "figure.hiking", 35: "figure.rower", 37: "figure.run", 44: "figure.stair.stepper",
        46: "figure.pool.swim", 50: "figure.strengthtraining.traditional", 52: "figure.walk", 57: "figure.yoga",
        59: "figure.core.training", 62: "figure.flexibility", 63: "figure.highintensity.intervaltraining",
        73: "figure.mixed.cardio", 80: "figure.cooldown",
    ]

    static func name(_ activityType: Int) -> String {
        names[activityType] ?? String(localized: "Training")
    }

    static func symbol(_ activityType: Int) -> String {
        symbols[activityType] ?? "flame.fill"
    }
}
