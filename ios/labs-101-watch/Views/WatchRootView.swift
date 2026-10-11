import SwiftUI

struct WatchRootView: View {
    @State private var model = WatchWorkoutModel.shared

    var body: some View {
        Group {
            if model.state != nil {
                WorkoutPagesView(model: model)
            } else if let summary = model.summary {
                SummaryView(summary: summary, onClose: model.closeSummary)
            } else {
                IdleView(model: model)
            }
        }
        .alert("Fehler", isPresented: Binding { model.error != nil } set: { if !$0 { model.error = nil } }) {
            Button("OK") {}
        } message: {
            Text(model.error ?? "")
        }
    }
}

/// Controls, exercises and metrics side by side, like the workout app.
private struct WorkoutPagesView: View {
    let model: WatchWorkoutModel

    private enum Page {
        case controls, exercises, metrics
    }

    @State private var page = Page.exercises

    var body: some View {
        TabView(selection: $page) {
            ControlsView(model: model)
                .tag(Page.controls)
            NavigationStack {
                ExerciseListView(model: model)
            }
            .tag(Page.exercises)
            MetricsView(model: model)
                .tag(Page.metrics)
        }
        .tabViewStyle(.page)
    }
}

/// Shown until a training is started on the iPhone.
private struct IdleView: View {
    let model: WatchWorkoutModel

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.system(size: 40))
                    .foregroundStyle(.green)
                Text("Kein Training aktiv")
                    .font(.headline)
                Text(model.isPhoneReachable
                     ? "Starte ein Training auf dem iPhone, es erscheint dann hier."
                     : "Öffne die App auf dem iPhone und starte dort ein Training.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Button("Aktualisieren", systemImage: "arrow.clockwise", action: model.refresh)
                    .disabled(!model.isPhoneReachable)
            }
        }
    }
}

private struct SummaryView: View {
    let summary: WatchWorkoutModel.Summary
    let onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Label("Gespeichert", systemImage: "checkmark.circle.fill")
                    .font(.headline)
                    .foregroundStyle(.green)
                Text(summary.name)
                    .font(.title3.bold())
                MetricRow(title: "Zeit", value: summary.duration.workoutDuration, tint: .yellow)
                MetricRow(title: "Sätze", value: "\(summary.sets)", tint: .green)
                MetricRow(title: "Volumen", value: summary.volume.formattedVolume, tint: .orange)
                if summary.energy > 0 {
                    MetricRow(title: "Aktive kcal", value: Int(summary.energy).formatted(), tint: .pink)
                }
                Button("Fertig", action: onClose)
                    .padding(.top, 6)
            }
        }
    }
}

struct MetricRow: View {
    let title: LocalizedStringKey
    let value: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.title3.weight(.semibold))
                .foregroundStyle(tint)
                .monospacedDigit()
        }
    }
}

extension TimeInterval {
    /// "45:12" or "1:05:12"
    var workoutDuration: String {
        Duration.seconds(Int(self)).formatted(.time(pattern: self >= 3600 ? .hourMinuteSecond : .minuteSecond))
    }
}

extension Double {
    /// "850 kg", tonnes from 10 t on so it stays short
    var formattedVolume: String {
        self >= 10_000
            ? "\((self / 1000).formatted(.number.precision(.fractionLength(0...1)))) t"
            : "\(Int(rounded()).formatted()) kg"
    }

    /// 62.5 -> "62,5", 60.0 -> "60"
    var weightFormatted: String {
        formatted(.number.precision(.fractionLength(0...2)))
    }
}
