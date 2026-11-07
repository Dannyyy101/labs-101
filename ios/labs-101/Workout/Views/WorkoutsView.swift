import SwiftUI

/// Templates, the running workout and the history, the iOS counterpart of /workouts.
struct WorkoutsView: View {
    @Environment(\.workoutService) private var workoutService
    @State private var model = WorkoutOverviewModel()
    @State private var openSession: WorkoutSession?
    @State private var startingTemplate: Int??

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Training")
                .refreshable { await load() }
                .task { await load() }
                .navigationDestination(for: WorkoutSession.self) { session in
                    WorkoutSessionDetailView(session: session) {
                        Task { await model.delete(session, using: workoutService) }
                    }
                }
                .fullScreenCover(item: $openSession, onDismiss: reload) { session in
                    WorkoutSessionView(session: session, exercises: model.exercises, history: model.history,
                                       service: workoutService)
                }
                .alert("Fehler", isPresented: $model.actionError.isPresent) {} message: {
                    Text(model.actionError ?? "")
                }
                .onChange(of: WatchBridge.shared.workoutsEnded) { reload() }
        }
    }

    @ViewBuilder
    private var content: some View {
        if let loadError = model.loadError, !model.hasLoaded {
            ContentUnavailableView {
                Label("Keine Verbindung", systemImage: "wifi.exclamationmark")
            } description: {
                Text(loadError)
            } actions: {
                Button("Erneut versuchen") { reload() }
                    .buttonStyle(.borderedProminent)
            }
            .background { Backdrop() }
        } else if !model.hasLoaded {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background { Backdrop() }
        } else {
            List {
                Group {
                if let active = model.active {
                    Section {
                        ActiveWorkoutBanner(session: active) { openSession = active }
                    }
                }

                Section("Letzte 30 Tage") {
                    RecentStats(sessions: model.recent)
                }

                Section("Vorlagen") {
                    ForEach(model.templates) { template in
                        TemplateRow(template: template, last: model.lastSession(of: template),
                                    isStarting: startingTemplate == .some(template.id),
                                    disabled: model.active != nil || startingTemplate != nil) {
                            start(template.id)
                        }
                    }
                    Button {
                        start(nil)
                    } label: {
                        Label(startingTemplate == .some(nil) ? "Startet…" : "Freies Training", systemImage: "play.fill")
                    }
                    .disabled(model.active != nil || startingTemplate != nil)
                }

                Section("Trainings") {
                    ForEach(model.history) { session in
                        NavigationLink(value: session) {
                            SessionRow(session: session)
                        }
                        .swipeActions {
                            Button("Löschen", systemImage: "trash", role: .destructive) {
                                Task { await model.delete(session, using: workoutService) }
                            }
                        }
                    }
                    if model.history.isEmpty {
                        Text("Noch keine Trainings im letzten Jahr")
                            .foregroundStyle(.secondary)
                    }
                }
                }
                .glassRows()
            }
            .glassList()
        }
    }

    private func start(_ templateID: Int?) {
        startingTemplate = .some(templateID)
        Task {
            defer { startingTemplate = nil }
            openSession = await model.start(templateID: templateID, using: workoutService)
            if openSession != nil {
                WatchBridge.shared.startWatchApp()
            }
        }
    }

    private func reload() {
        Task { await load() }
    }

    private func load() async {
        await model.load(using: workoutService)
        // for exercises added on the watch and its "last time" hints
        WatchBridge.shared.use(exercises: model.exercises, history: model.history)
    }
}

private struct ActiveWorkoutBanner: View {
    let session: WorkoutSession
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 14) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.title2)
                    .symbolEffect(.pulse)
                VStack(alignment: .leading) {
                    Text("Training läuft")
                        .font(.subheadline)
                        .opacity(0.85)
                    Text(session.name)
                        .font(.headline)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text(session.startedAt, style: .timer)
                        .font(.title3.bold())
                        .monospacedDigit()
                    let sets = session.exercises.flatMap(\.sets)
                    Text("\(sets.filter(\.done).count) / \(sets.count) Sätze")
                        .font(.subheadline)
                        .opacity(0.85)
                }
                Image(systemName: "chevron.right")
            }
            .foregroundStyle(Color.onBrand)
        }
        // the running object is filled with the brand, like a solid icon tile on the web
        .listRowBackground(Color.brand)
    }
}

private struct RecentStats: View {
    let sessions: [WorkoutSession]

    var body: some View {
        let sets = sessions.flatMap(\.doneSets)
        Grid(horizontalSpacing: 16, verticalSpacing: 12) {
            GridRow {
                stat("Trainings", "\(sessions.count)", "figure.strengthtraining.traditional")
                stat("Zeit", sessions.reduce(0) { $0 + $1.duration() }.workoutHours, "clock")
            }
            GridRow {
                stat("Volumen", WorkoutStats.formattedVolume(WorkoutStats.volume(sets)), "scalemass")
                stat("Sätze", "\(sets.count)", "checklist")
            }
        }
        .padding(.vertical, 4)
    }

    private func stat(_ title: LocalizedStringKey, _ value: String, _ systemImage: String) -> some View {
        HStack(spacing: 10) {
            IconTile(systemImage: systemImage)
            VStack(alignment: .leading, spacing: 0) {
                Text(title)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Color.inkMuted)
                Text(value)
                    .font(.title3.bold())
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

private struct TemplateRow: View {
    let template: WorkoutTemplate
    let last: WorkoutSession?
    let isStarting: Bool
    let disabled: Bool
    let onStart: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(template.name)
                    .font(.headline)
                Text(summary)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                if template.sharedWithMe == true {
                    Label("Geteilt von \(template.ownerName ?? "einem Freund")", systemImage: "person.2.fill")
                        .font(.caption)
                        .foregroundStyle(Color.brandStrong)
                }
                Text(last.map { "Zuletzt \($0.startedAt.formatted(.dateTime.day().month()))" } ?? "Noch nie trainiert")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Button(action: onStart) {
                // a fixed width, so the label stays centred when it switches to "Startet…"
                Label(isStarting ? "Startet…" : "Starten", systemImage: "play.fill")
                    .lineLimit(1)
                    .frame(minWidth: 96)
            }
            .buttonStyle(.borderedProminent)
            .foregroundStyle(Color.onBrand)
            .fixedSize()
            .disabled(disabled)
        }
    }

    private var summary: String {
        var parts = [count(template.workoutExercises.count, "Übung", "Übungen"), count(template.setCount, "Satz", "Sätze")]
        if template.cardioMinutes > 0 {
            parts.append("\(template.cardioMinutes) min Cardio")
        }
        return parts.joined(separator: " · ")
    }

    private func count(_ value: Int, _ one: String, _ many: String) -> String {
        "\(value) \(value == 1 ? one : many)"
    }
}

private struct SessionRow: View {
    let session: WorkoutSession

    var body: some View {
        let sets = session.doneSets
        VStack(alignment: .leading, spacing: 2) {
            Text(session.name)
                .font(.headline)
            Text("\(session.startedAt.formatted(.dateTime.weekday(.abbreviated).day().month().hour().minute())) · \(session.duration().workoutDuration)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text("\(sets.count) Sätze · \(WorkoutStats.formattedVolume(WorkoutStats.volume(sets)))" + (session.cardioMinutes > 0 ? " · \(session.cardioMinutes) min Cardio" : ""))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

/// The done sets of a finished workout.
struct WorkoutSessionDetailView: View {
    let session: WorkoutSession
    let onDelete: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var confirmDelete = false

    var body: some View {
        List {
            Group {
            Section {
                LabeledContent("Datum", value: session.startedAt.formatted(date: .long, time: .shortened))
                LabeledContent("Dauer", value: session.duration().workoutDuration)
                LabeledContent("Volumen", value: WorkoutStats.formattedVolume(WorkoutStats.volume(session.doneSets)))
            }
            ForEach(session.exercises) { exercise in
                Section(exercise.name) {
                    if let note = exercise.note {
                        Label(note, systemImage: "note.text")
                            .foregroundStyle(.secondary)
                    }
                    if let cardio = exercise.cardio {
                        LabeledContent("Cardio", value: "\(cardio.minutes) min")
                            .monospacedDigit()
                    }
                    ForEach(Array(exercise.sets.filter(\.done).enumerated()), id: \.offset) { index, set in
                        LabeledContent("Satz \(index + 1)", value: set.formatted)
                            .monospacedDigit()
                    }
                }
            }
            Section {
                Button("Training löschen", role: .destructive) { confirmDelete = true }
                    .frame(maxWidth: .infinity)
            }
            }
            .glassRows()
        }
        .glassList()
        .navigationTitle(session.name)
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog("„\(session.name)“ löschen?", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("Löschen", role: .destructive) {
                onDelete()
                dismiss()
            }
        }
    }
}

#Preview {
    WorkoutsView()
}
