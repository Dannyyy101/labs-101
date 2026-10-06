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
                .refreshable { await model.load(using: workoutService) }
                .task { await model.load(using: workoutService) }
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
        } else if !model.hasLoaded {
            ProgressView()
        } else {
            List {
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
        }
    }

    private func start(_ templateID: Int?) {
        startingTemplate = .some(templateID)
        Task {
            defer { startingTemplate = nil }
            openSession = await model.start(templateID: templateID, using: workoutService)
        }
    }

    private func reload() {
        Task { await model.load(using: workoutService) }
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
            .foregroundStyle(.white)
        }
        .listRowBackground(Color.green)
    }
}

private struct RecentStats: View {
    let sessions: [WorkoutSession]

    var body: some View {
        let sets = sessions.flatMap(\.doneSets)
        Grid(horizontalSpacing: 16, verticalSpacing: 12) {
            GridRow {
                stat("Trainings", "\(sessions.count)", .pink)
                stat("Zeit", sessions.reduce(0) { $0 + $1.duration() }.workoutHours, .blue)
            }
            GridRow {
                stat("Volumen", WorkoutStats.formattedVolume(WorkoutStats.volume(sets)), .orange)
                stat("Sätze", "\(sets.count)", .green)
            }
        }
        .padding(.vertical, 4)
    }

    private func stat(_ title: LocalizedStringKey, _ value: String, _ tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(tint)
                .textCase(.uppercase)
            Text(value)
                .font(.title3.bold())
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
                Text("\(count(template.workoutExercises.count, "Übung", "Übungen")) · \(count(template.setCount, "Satz", "Sätze"))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text(last.map { "Zuletzt \($0.startedAt.formatted(.dateTime.day().month()))" } ?? "Noch nie trainiert")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Button(isStarting ? "Startet…" : "Starten", systemImage: "play.fill", action: onStart)
                .buttonStyle(.borderedProminent)
                .tint(.green)
                .foregroundStyle(.white)
                .disabled(disabled)
        }
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
            Text("\(sets.count) Sätze · \(WorkoutStats.formattedVolume(WorkoutStats.volume(sets)))")
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
            Section {
                LabeledContent("Datum", value: session.startedAt.formatted(date: .long, time: .shortened))
                LabeledContent("Dauer", value: session.duration().workoutDuration)
                LabeledContent("Volumen", value: WorkoutStats.formattedVolume(WorkoutStats.volume(session.doneSets)))
            }
            ForEach(session.exercises) { exercise in
                Section(exercise.name) {
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
