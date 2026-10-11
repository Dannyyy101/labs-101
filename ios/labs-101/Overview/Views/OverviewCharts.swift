import Charts
import SwiftUI

/// A score as a ring that fills up to 100.
struct ScoreRing<Label: View>: View {
    let value: Int?
    let color: Color
    var size: CGFloat = 84
    var lineWidth: CGFloat = 9
    @ViewBuilder let label: Label

    private var progress: Double {
        guard let value else { return 0 }
        return max(0.005, min(1, Double(value) / 100))
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(color.opacity(0.16), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.smooth, value: progress)
            label
        }
        .frame(width: size, height: size)
    }
}

extension SleepStage {
    /// The rows of the hypnogram, awake at the top and deep sleep at the bottom like in Apple Health.
    static let rows: [SleepStage] = [.awake, .rem, .core, .deep]

    /// sleep without a recorded stage is drawn as core sleep
    var row: SleepStage { self == .asleep ? .core : self }

    var label: String {
        switch row {
        case .awake: String(localized: "Wach")
        case .rem: String(localized: "REM")
        case .deep: String(localized: "Tief")
        case .core, .asleep: String(localized: "Kern")
        }
    }

    var color: Color {
        switch row {
        case .awake: Color(red: 1, green: 0.62, blue: 0.04)
        case .rem: Color(red: 0.39, green: 0.82, blue: 1)
        case .deep: Color(red: 0.37, green: 0.36, blue: 0.9)
        case .core, .asleep: Color(red: 0.04, green: 0.52, blue: 1)
        }
    }
}

/// The stages of the night over time.
struct Hypnogram: View {
    let sleep: Sleep

    var body: some View {
        Chart(sleep.stages, id: \.self) { stage in
            RectangleMark(
                xStart: .value("Beginn", max(stage.start, sleep.start)),
                xEnd: .value("Ende", min(stage.end, sleep.end)),
                y: .value("Phase", stage.stage.label),
                height: .ratio(0.7)
            )
            .foregroundStyle(stage.stage.color)
            .clipShape(.rect(cornerRadius: 3))
        }
        .chartYScale(domain: SleepStage.rows.map(\.label))
        .chartXScale(domain: sleep.start...max(sleep.end, sleep.start.addingTimeInterval(60)))
        .chartXAxis {
            AxisMarks(values: .stride(by: .hour, count: sleep.end.timeIntervalSince(sleep.start) > 6 * 3600 ? 2 : 1)) {
                AxisGridLine()
                AxisValueLabel(format: .dateTime.hour(.twoDigits(amPM: .omitted)).minute())
            }
        }
        .frame(height: 150)
        .accessibilityLabel("Schlafphasen")
    }
}

/// Heart rate of the whole day with the resting heart rate as a dashed line.
struct HeartRateChart: View {
    let points: [HeartRatePoint]
    let day: Date
    let resting: Double?
    var calendar: Calendar = .current

    @State private var selection: Date?

    /// more than 20 minutes without a reading leaves a gap
    private var segments: [(segment: Int, point: HeartRatePoint)] {
        var segment = 0
        return points.enumerated().map { index, point in
            if index > 0, point.date.timeIntervalSince(points[index - 1].date) > 20 * 60 {
                segment += 1
            }
            return (segment, point)
        }
    }

    private var domain: ClosedRange<Double> {
        let values = points.map(\.bpm)
        let lo = ((min(values.min() ?? 50, resting ?? .infinity) - 8) / 10).rounded(.down) * 10
        let hi = (((values.max() ?? 100) + 5) / 10).rounded(.up) * 10
        return lo...hi
    }

    private var selected: HeartRatePoint? {
        guard let selection else { return nil }
        return points.min { abs($0.date.timeIntervalSince(selection)) < abs($1.date.timeIntervalSince(selection)) }
    }

    var body: some View {
        if points.count < 2 {
            Text("Keine Herzfrequenz an diesem Tag")
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, minHeight: 160)
        } else {
            let end = calendar.date(byAdding: .day, value: 1, to: day) ?? day
            Chart {
                ForEach(segments, id: \.point.date) { item in
                    LineMark(
                        x: .value("Zeit", item.point.date),
                        y: .value("Herzfrequenz", item.point.bpm),
                        series: .value("Abschnitt", item.segment)
                    )
                    .foregroundStyle(Color.heart)
                    .lineStyle(StrokeStyle(lineWidth: 1.5, lineJoin: .round))
                }
                if let resting {
                    RuleMark(y: .value("Ruhepuls", resting))
                        .foregroundStyle(Color.heart.opacity(0.6))
                        .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))
                        .annotation(position: .top, alignment: .trailing) {
                            Text("Ruhepuls \(Int(resting.rounded()))")
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(Color.heart)
                        }
                }
                if let selected {
                    RuleMark(x: .value("Zeit", selected.date))
                        .foregroundStyle(.secondary.opacity(0.4))
                        .annotation(position: .top, overflowResolution: .init(x: .fit(to: .chart), y: .disabled)) {
                            VStack(spacing: 0) {
                                Text("\(Int(selected.bpm.rounded())) bpm")
                                    .font(.caption.weight(.semibold))
                                Text(selected.date, format: .dateTime.hour().minute())
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            .monospacedDigit()
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(.background.secondary, in: .rect(cornerRadius: 8))
                        }
                    PointMark(x: .value("Zeit", selected.date), y: .value("Herzfrequenz", selected.bpm))
                        .foregroundStyle(Color.heart)
                }
            }
            .chartXScale(domain: day...end)
            .chartYScale(domain: domain)
            .chartXAxis {
                AxisMarks(values: .stride(by: .hour, count: 6)) {
                    AxisGridLine()
                    AxisValueLabel(format: .dateTime.hour(.twoDigits(amPM: .omitted)).minute())
                }
            }
            .chartXSelection(value: $selection)
            .frame(height: 180)
            .accessibilityLabel("Herzfrequenz im Tagesverlauf")
        }
    }
}

/// One bar per day of the history, the shown day is highlighted, a tap opens the day.
struct TrendBars: View {
    let title: LocalizedStringKey
    let history: [DayScore]
    let selectedDay: String
    let value: (DayScore) -> Double?
    let color: Color
    let format: (Double) -> String
    var max: Double?
    let onSelect: (String) -> Void

    var body: some View {
        let values = history.map(value)
        let shown = values.compactMap { $0 }
        let top = max ?? Swift.max(1, shown.max() ?? 0) * 1.1
        let average = shown.isEmpty ? nil : shown.reduce(0, +) / Double(shown.count)

        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(title)
                    .fontWeight(.semibold)
                Spacer()
                Text("Ø 14 Tage")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text(average.map(format) ?? "–")
                    .font(.subheadline.weight(.semibold))
                    .monospacedDigit()
            }

            HStack(alignment: .bottom, spacing: 3) {
                ForEach(Array(history.enumerated()), id: \.element.id) { index, day in
                    let v = values[index]
                    Button {
                        onSelect(day.date)
                    } label: {
                        GeometryReader { proxy in
                            VStack {
                                Spacer(minLength: 0)
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(v != nil ? color : Color(.quaternarySystemFill))
                                    .frame(height: v.map { Swift.max(3, $0 / top * proxy.size.height) } ?? 3)
                                    .opacity(day.date == selectedDay ? 1 : 0.45)
                            }
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(shortDay(day.date)): \(v.map(format) ?? String(localized: "keine Daten"))")
                }
            }
            .frame(height: 64)

            if let first = history.first, let last = history.last {
                HStack {
                    Text(shortDay(first.date))
                    Spacer()
                    Text(shortDay(last.date))
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    /// "2026-10-04" -> "Sa., 4."
    private func shortDay(_ day: String) -> String {
        Date(isoDay: day)?.formatted(.dateTime.weekday(.abbreviated).day()) ?? day
    }
}
