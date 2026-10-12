import SwiftUI

/// Sleep, recovery and strain of a day, the iOS counterpart of the home page in the web.
struct HealthOverviewView: View {
    @Environment(\.overviewService) private var overviewService
    @State private var model = HealthOverviewModel()

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(model.day.dayTitle())
                .navigationSubtitle(model.day.longDay)
                .toolbar { toolbar }
                .refreshable { await model.load(using: overviewService) }
                .task(id: model.day) { await model.load(using: overviewService) }
        }
    }

    @ViewBuilder
    private var content: some View {
        if let overview = model.overview {
            OverviewList(overview: overview, day: model.day, calendar: model.calendar) { day in
                if let date = Date(isoDay: day, calendar: model.calendar) {
                    model.show(date)
                }
            }
            .overlay(alignment: .top) {
                if model.isLoading {
                    ProgressView()
                        .padding(.top, 8)
                }
            }
        } else if let loadError = model.loadError {
            ContentUnavailableView {
                Label("Keine Verbindung", systemImage: "wifi.exclamationmark")
            } description: {
                Text(loadError)
            } actions: {
                Button("Erneut versuchen") {
                    Task { await model.load(using: overviewService) }
                }
                .buttonStyle(.borderedProminent)
            }
        } else {
            ProgressView()
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItemGroup(placement: .topBarLeading) {
            Button("Vorheriger Tag", systemImage: "chevron.left") {
                model.showDay(byAdding: -1)
            }
            Button("Nächster Tag", systemImage: "chevron.right") {
                model.showDay(byAdding: 1)
            }
            .disabled(model.isToday)
        }
        if !model.isToday {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Heute") { model.showToday() }
            }
        }
    }
}

private let goals = (steps: 10_000.0, exercise: 30.0, stand: 12.0)

private struct OverviewList: View {
    let overview: HealthOverview
    let day: Date
    let calendar: Calendar
    let onSelectDay: (String) -> Void

    var body: some View {
        let recovery = overview.recovery, strain = overview.strain, activity = overview.activity
        let target = OverviewStatus.strainTarget(recovery.score)

        List {
            if !overview.hasData {
                Section {
                    ContentUnavailableView(
                        "Noch keine Gesundheitsdaten",
                        systemImage: "heart.text.square",
                        description: Text("Sobald die App Schlaf, Herzfrequenz und Aktivität aus Apple Health synchronisiert hat, erscheinen sie hier.")
                    )
                }
            }

            Section {
                HStack(spacing: 0) {
                    ScoreColumn(title: "Belastung", score: strain.score,
                                colors: [Color(red: 1, green: 0.85, blue: 0.45), .strain, Color(red: 0.95, green: 0.4, blue: 0.15)],
                                target: target)
                    Divider()
                    ScoreColumn(title: "Erholung", score: recovery.score,
                                colors: OverviewStatus.recoveryGradient(recovery.score))
                    Divider()
                    ScoreColumn(title: "Schlaf", score: overview.sleep?.score,
                                colors: [.sleep, Color(red: 0.62, green: 0.68, blue: 0.97)])
                }
                .padding(.vertical, 8)
            }

            Section {
                if let sleep = overview.sleep {
                    SleepDetail(sleep: sleep)
                } else {
                    EmptyRow(systemImage: "moon.zzz", text: "Für diese Nacht wurde kein Schlaf aufgezeichnet.")
                }
            } header: {
                SectionHeader("Schlaf", trailing: overview.sleep.map {
                    "\($0.start.formatted(date: .omitted, time: .shortened)) – \($0.end.formatted(date: .omitted, time: .shortened))"
                })
            }

            Section {
                VitalRow(label: "Herzfrequenzvariabilität", value: recovery.hrv, baseline: recovery.hrvBaseline, unit: "ms", higherIsBetter: true)
                VitalRow(label: "Ruhepuls", value: recovery.restingHeartRate, baseline: recovery.restingHeartRateBaseline, unit: "bpm")
                VitalRow(label: "Atemfrequenz", value: recovery.respiratoryRate, baseline: recovery.respiratoryRateBaseline, unit: "/min", decimals: 1, neutral: true)
                VitalRow(label: "Blutsauerstoff", value: recovery.oxygenSaturation.map { $0 * 100 }, unit: "%", decimals: 1)
                VitalRow(label: "Handgelenktemperatur", value: recovery.wristTemperatureDeviation, unit: "°C", decimals: 2, signed: true)
            } header: {
                SectionHeader("Vitalwerte", trailing: String(localized: "vs. Ø 30 Tage"))
            }

            Section {
                HeartRateChart(points: overview.heartRatePoints, day: day, resting: recovery.restingHeartRate, calendar: calendar)
                    .padding(.vertical, 4)
                ZonesView(seconds: strain.zoneSeconds, max: strain.maxHeartRate)
            } header: {
                SectionHeader("Herzfrequenz", trailing: strain.averageHeartRate.map {
                    "Ø \(Int($0.rounded())) · max \(Int((strain.peakHeartRate ?? 0).rounded())) bpm"
                })
            }

            Section("Aktivität") {
                GoalRow(systemImage: "figure.walk", label: "Schritte", value: activity.steps, goal: goals.steps,
                        color: .recoveryHigh) { $0.roundedFormatted }
                GoalRow(systemImage: "flame.fill", label: "Trainingsminuten", value: activity.exerciseMinutes, goal: goals.exercise,
                        color: Color(red: 0.64, green: 0.97, blue: 0)) { "\(Int($0.rounded())) min" }
                GoalRow(systemImage: "figure.stand", label: "Stehstunden", value: Double(activity.standHours), goal: goals.stand,
                        color: Color(red: 0.39, green: 0.82, blue: 1)) { "\(Int($0)) h" }
                StatGrid(stats: [
                    ("Aktive Energie", activity.activeEnergy.roundedFormatted, "kcal"),
                    ("Gesamt", (activity.activeEnergy + activity.basalEnergy).roundedFormatted, "kcal"),
                    ("Distanz", activity.distance.kilometers(), "km"),
                    ("Stockwerke", activity.flightsClimbed.roundedFormatted, nil),
                    ("Tageslicht", (activity.daylightMinutes * 60).sleepDuration, nil),
                    ("Gegessen", activity.energyConsumed?.roundedFormatted ?? "–", activity.energyConsumed != nil ? "kcal" : nil),
                ])
            }

            if !overview.history.isEmpty {
                Section("Verlauf") {
                    TrendBars(title: "Erholung", history: overview.history, selectedDay: overview.date,
                              value: { $0.recovery.map(Double.init) }, color: .recoveryHigh,
                              format: { "\(Int($0.rounded())) %" }, max: 100, onSelect: onSelectDay)
                    TrendBars(title: "Belastung", history: overview.history, selectedDay: overview.date,
                              value: { $0.strain.map(Double.init) }, color: .strain,
                              format: { "\(Int($0.rounded())) %" }, max: 100, onSelect: onSelectDay)
                    TrendBars(title: "Schlaf", history: overview.history, selectedDay: overview.date,
                              value: \.sleep, color: .sleep,
                              format: \.sleepDuration, onSelect: onSelectDay)
                    TrendBars(title: "HRV", history: overview.history, selectedDay: overview.date,
                              value: \.hrv, color: .hrv,
                              format: { "\(Int($0.rounded())) ms" }, onSelect: onSelectDay)
                }
            }

            Section {
                if overview.workouts.isEmpty {
                    EmptyRow(systemImage: "dumbbell", text: "Kein Training an diesem Tag.")
                } else {
                    ForEach(overview.workouts) { WorkoutRow(workout: $0) }
                }
            } header: {
                SectionHeader("Trainings", trailing: overview.workouts.isEmpty ? nil : "\(overview.workouts.count)")
            }

            Section("Körper") {
                LabeledContent("VO₂max", value: overview.vitals.vo2Max.map { "\($0.decimalFormatted(1)) ml/kg·min" } ?? "–")
                    .monospacedDigit()
                LabeledContent {
                    VStack(alignment: .trailing, spacing: 0) {
                        Text(overview.vitals.bodyMass.map { "\($0.decimalFormatted(1)) kg" } ?? "–")
                        if let change = overview.vitals.bodyMassChange, abs(change) >= 0.05 {
                            Text("\(change > 0 ? "+" : "−")\(abs(change).decimalFormatted(1)) kg in 30 Tagen")
                                .font(.caption)
                        }
                    }
                    .monospacedDigit()
                } label: {
                    Text("Gewicht")
                }
            }
        }
        .listSectionSpacing(.compact)
    }
}

// MARK: Rows

private struct SectionHeader: View {
    let title: LocalizedStringKey
    let trailing: String?

    init(_ title: LocalizedStringKey, trailing: String?) {
        self.title = title
        self.trailing = trailing
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
            Spacer()
            if let trailing {
                Text(trailing)
                    .textCase(nil)
                    .monospacedDigit()
            }
        }
    }
}

private struct ScoreColumn: View {
    let title: LocalizedStringKey
    let score: Int?
    let colors: [Color]
    var target: ClosedRange<Int>?

    var body: some View {
        VStack(spacing: 8) {
            ScoreRing(value: score, colors: colors, target: target, size: 80, lineWidth: 10) {
                HStack(alignment: .firstTextBaseline, spacing: 1) {
                    Text(score.map(String.init) ?? "–")
                        .font(.title2.weight(.semibold))
                    if score != nil {
                        Text("%")
                            .font(.subheadline.weight(.medium))
                    }
                }
                .monospacedDigit()
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            }
            Text(title)
                .font(.headline.weight(.medium))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }
}

private struct SleepDetail: View {
    let sleep: Sleep

    private var parts: [(stage: SleepStage, seconds: Double)] {
        [(.awake, sleep.awake), (.rem, sleep.rem), (.core, sleep.core), (.deep, sleep.deep)]
    }

    var body: some View {
        let total = parts.reduce(0) { $0 + $1.seconds }
        let staged = sleep.deep + sleep.core + sleep.rem

        StatGrid(stats: [
            ("Geschlafen", sleep.asleep.sleepDuration, nil),
            ("Im Bett", sleep.inBed.sleepDuration, nil),
            ("Effizienz", sleep.inBed > 0 ? min(1, (sleep.inBed - sleep.awake) / sleep.inBed).percentFormatted() : "–", nil),
            ("Atemfrequenz", sleep.respiratoryRate?.decimalFormatted(1) ?? "–", sleep.respiratoryRate != nil ? "/min" : nil),
        ])

        Hypnogram(sleep: sleep)
            .padding(.vertical, 4)

        if staged > 0, total > 0 {
            VStack(spacing: 12) {
                GeometryReader { proxy in
                    HStack(spacing: 0) {
                        ForEach(parts, id: \.stage) { part in
                            part.stage.color
                                .frame(width: proxy.size.width * part.seconds / total)
                        }
                    }
                }
                .frame(height: 10)
                .clipShape(.capsule)

                Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 10) {
                    GridRow { stageCell(parts[0], total); stageCell(parts[1], total) }
                    GridRow { stageCell(parts[2], total); stageCell(parts[3], total) }
                }
            }
            .padding(.vertical, 4)
        }
    }

    private func stageCell(_ part: (stage: SleepStage, seconds: Double), _ total: Double) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 6) {
                Circle().fill(part.stage.color).frame(width: 8, height: 8)
                Text(part.stage.label)
            }
            .font(.subheadline.weight(.medium))
            .foregroundStyle(.secondary)
            Text("\(Text(part.seconds.sleepDuration).fontWeight(.semibold)) \(Text((part.seconds / total).percentFormatted()).font(.subheadline).foregroundStyle(.secondary))")
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

/// Two columns of label and value.
private struct StatGrid: View {
    let stats: [(LocalizedStringKey, String, String?)]

    var body: some View {
        Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 12) {
            ForEach(Array(stride(from: 0, to: stats.count, by: 2)), id: \.self) { index in
                GridRow {
                    cell(stats[index])
                    if index + 1 < stats.count {
                        cell(stats[index + 1])
                    }
                }
            }
        }
        .padding(.vertical, 4)
    }

    private func cell(_ stat: (LocalizedStringKey, String, String?)) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(stat.0)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(stat.1)
                    .font(.title3.weight(.semibold))
                if let unit = stat.2 {
                    Text(unit)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            .monospacedDigit()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

private struct VitalRow: View {
    let label: LocalizedStringKey
    let value: Double?
    var baseline: Double?
    let unit: String
    var decimals = 0
    var higherIsBetter = false
    var neutral = false
    var signed = false

    var body: some View {
        let change = value.flatMap { value in baseline.flatMap { $0 != 0 ? (value - $0) / $0 : nil } }
        let good = change.map { higherIsBetter ? $0 > 0 : $0 < 0 } ?? false

        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text(value.map { "\(signed && $0 > 0 ? "+" : "")\($0.decimalFormatted(decimals))" } ?? "–")
                        .font(.title3.weight(.semibold))
                    if value != nil {
                        Text(unit)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            Spacer()
            if let baseline {
                VStack(alignment: .trailing, spacing: 2) {
                    if let change, abs(change) >= 0.02 {
                        Label(abs(change).percentFormatted(), systemImage: change > 0 ? "arrow.up.right" : "arrow.down.right")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(neutral ? .secondary : good ? Color.recoveryHigh : Color.recoveryLow)
                    }
                    Text("Ø \(baseline.decimalFormatted(decimals))")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .monospacedDigit()
        .accessibilityElement(children: .combine)
    }
}

private struct ZonesView: View {
    private static let zones: [(key: String, color: Color)] = [
        ("Z1", Color(red: 0.39, green: 0.82, blue: 1)),
        ("Z2", Color(red: 0.19, green: 0.82, blue: 0.35)),
        ("Z3", Color(red: 1, green: 0.84, blue: 0.04)),
        ("Z4", Color(red: 1, green: 0.62, blue: 0.04)),
        ("Z5", Color(red: 1, green: 0.27, blue: 0.23)),
    ]

    let seconds: [Double]
    let max: Double

    var body: some View {
        let total = seconds.reduce(0, +)
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text("Herzfrequenzzonen")
                    .fontWeight(.semibold)
                Spacer()
                Text("max. \(Int(max.rounded())) bpm")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            HStack(alignment: .top, spacing: 8) {
                ForEach(Self.zones.indices, id: \.self) { index in
                    let value = index < seconds.count ? seconds[index] : 0
                    VStack(alignment: .leading, spacing: 4) {
                        Capsule()
                            .fill(Self.zones[index].color)
                            .opacity(total > 0 && value > 0 ? 1 : 0.25)
                            .frame(height: 6)
                        Text("\(Self.zones[index].key) · \(Int((max * (0.5 + Double(index) * 0.1)).rounded()))+")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Text(value >= 60 ? value.sleepDuration : "–")
                            .font(.caption.weight(.semibold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .monospacedDigit()
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private struct GoalRow: View {
    let systemImage: String
    let label: LocalizedStringKey
    let value: Double
    let goal: Double
    let color: Color
    let format: (Double) -> String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.body.weight(.semibold))
                .foregroundStyle(color)
                .frame(width: 36, height: 36)
                .background(color.opacity(0.15), in: .circle)
            VStack(spacing: 6) {
                HStack(alignment: .firstTextBaseline) {
                    Text(label)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("\(Text(format(value)).fontWeight(.semibold).foregroundStyle(.primary)) / \(format(goal))")
                        .foregroundStyle(.secondary)
                }
                .font(.subheadline)
                .monospacedDigit()
                ProgressView(value: min(value, goal), total: goal)
                    .tint(color)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct WorkoutRow: View {
    let workout: OverviewWorkout

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: WorkoutActivity.symbol(workout.activityType))
                .font(.body.weight(.semibold))
                .foregroundStyle(Color.strain)
                .frame(width: 40, height: 40)
                .background(Color.strain.opacity(0.15), in: .circle)
            VStack(alignment: .leading, spacing: 2) {
                Text(WorkoutActivity.name(workout.activityType))
                    .font(.headline)
                Text("\(workout.startDate.formatted(date: .omitted, time: .shortened)) – \(workout.endDate.formatted(date: .omitted, time: .shortened))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(workout.duration.sleepDuration)
                    .fontWeight(.semibold)
                Text(details)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
    }

    private var details: String {
        [
            workout.distance.flatMap { $0 > 0 ? "\($0.kilometers(decimals: 2)) km" : nil },
            workout.energy.flatMap { $0 > 0 ? "\($0.roundedFormatted) kcal" : nil },
            workout.averageHeartRate.flatMap { $0 > 0 ? "Ø \(Int($0.rounded())) bpm" : nil },
        ]
        .compactMap { $0 }
        .joined(separator: " · ")
    }
}

private struct EmptyRow: View {
    let systemImage: String
    let text: LocalizedStringKey

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.title2)
            Text(text)
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
    }
}

#Preview {
    HealthOverviewView()
}
