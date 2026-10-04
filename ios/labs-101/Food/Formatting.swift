import Foundation

extension Double {
    /// 1959.4 -> "1.959" (with the grouping of the current locale)
    var roundedFormatted: String {
        Int(rounded()).formatted()
    }

    /// 12.5 -> "12,5", 12.0 -> "12"
    var compactFormatted: String {
        formatted(.number.precision(.fractionLength(0...1)))
    }
}

extension Date {
    /// "Heute", "Gestern" or the weekday
    func dayTitle(calendar: Calendar = .current) -> String {
        if calendar.isDateInToday(self) { return String(localized: "Heute") }
        if calendar.isDateInYesterday(self) { return String(localized: "Gestern") }
        if calendar.isDateInTomorrow(self) { return String(localized: "Morgen") }
        return formatted(.dateTime.weekday(.wide))
    }

    /// "Sonntag, 4. Oktober"
    var longDay: String {
        formatted(.dateTime.weekday(.wide).day().month(.wide))
    }
}
