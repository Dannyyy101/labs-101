import SwiftUI

/// The exercises of the running workout, a tap opens the tracker of one.
struct ExerciseListView: View {
    let model: WatchWorkoutModel

    var body: some View {
        List {
            if let state = model.state {
                Section {
                    HStack {
                        Text(state.startedAt, style: .timer)
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(.yellow)
                            .monospacedDigit()
                        Spacer()
                        HeartRate(bpm: model.workout.heartRate)
                    }
                    .listRowBackground(Color.clear)
                }

                ForEach(state.exercises) { exercise in
                    NavigationLink(value: exercise.id) {
                        ExerciseRow(exercise: exercise)
                    }
                }

                if state.exercises.isEmpty {
                    Text("Noch keine Übung")
                        .foregroundStyle(.secondary)
                }

                NavigationLink {
                    AddExerciseView(model: model)
                } label: {
                    Label("Übung hinzufügen", systemImage: "plus")
                }
            }
        }
        .navigationTitle(model.state?.name ?? "")
        .navigationDestination(for: UUID.self) { id in
            ExerciseTrackerView(model: model, exerciseID: id)
        }
    }
}

private struct ExerciseRow: View {
    let exercise: WatchWorkoutState.Exercise

    var body: some View {
        let done = exercise.sets.filter(\.done).count
        let finished = !exercise.sets.isEmpty && done == exercise.sets.count
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Text(exercise.name)
                    .font(.headline)
                    .lineLimit(2)
                if finished {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                }
            }
            Text("\(done) / \(exercise.sets.count) Sätze")
                .font(.footnote)
                .foregroundStyle(finished ? .green : .secondary)
        }
    }
}

struct HeartRate: View {
    let bpm: Double?

    var body: some View {
        HStack(spacing: 2) {
            Text(bpm.map { Int($0).formatted() } ?? "--")
                .monospacedDigit()
            Image(systemName: "heart.fill")
                .foregroundStyle(.red)
                .symbolEffect(.pulse, isActive: bpm != nil)
        }
        .font(.headline)
    }
}

/// Time, heart rate and the numbers of the training so far.
struct MetricsView: View {
    let model: WatchWorkoutModel

    var body: some View {
        if let state = model.state {
            ScrollView {
                VStack(alignment: .leading, spacing: 6) {
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        Text(context.date.timeIntervalSince(state.startedAt).workoutDuration)
                            .font(.system(.title, design: .rounded).weight(.semibold))
                            .foregroundStyle(.yellow)
                            .monospacedDigit()
                    }
                    HeartRate(bpm: model.workout.heartRate)
                        .font(.title3)
                    MetricRow(title: "Aktive kcal", value: Int(model.workout.activeEnergy).formatted(), tint: .pink)
                    MetricRow(title: "Sätze", value: "\(state.doneSets.count) / \(state.allSets.count)", tint: .green)
                    MetricRow(title: "Volumen", value: state.volume.formattedVolume, tint: .orange)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

/// Finish or discard, left of the exercises like in the workout app.
struct ControlsView: View {
    let model: WatchWorkoutModel

    @State private var confirmFinish = false
    @State private var confirmDiscard = false

    var body: some View {
        let done = model.state?.doneSets.count ?? 0
        VStack(spacing: 10) {
            Button {
                confirmFinish = true
            } label: {
                Label("Beenden", systemImage: "checkmark")
                    .frame(maxWidth: .infinity)
            }
            .tint(.green)
            .disabled(done == 0)

            Button(role: .destructive) {
                confirmDiscard = true
            } label: {
                Label("Verwerfen", systemImage: "xmark")
                    .frame(maxWidth: .infinity)
            }

            if done == 0 {
                Text("Hake einen Satz ab, um das Training zu speichern.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .confirmationDialog("Training beenden?", isPresented: $confirmFinish) {
            Button("Beenden") { model.send(.finish) }
        } message: {
            Text(finishMessage)
        }
        .confirmationDialog("Training verwerfen?", isPresented: $confirmDiscard) {
            Button("Verwerfen", role: .destructive) { model.send(.discard) }
        } message: {
            Text("Alle erfassten Sätze gehen verloren.")
        }
    }

    private var finishMessage: String {
        let open = (model.state?.allSets.count ?? 0) - (model.state?.doneSets.count ?? 0)
        switch open {
        case 0: return String(localized: "Alle Sätze erledigt!")
        case 1: return String(localized: "1 Satz ist nicht abgehakt und wird nicht gespeichert.")
        default: return String(localized: "\(open) Sätze sind nicht abgehakt und werden nicht gespeichert.")
        }
    }
}

/// The exercises of the backend, the ones trained recently first.
struct AddExerciseView: View {
    let model: WatchWorkoutModel

    @Environment(\.dismiss) private var dismiss
    @State private var search = ""

    var body: some View {
        Group {
            if let catalog = model.catalog {
                let matches = search.isEmpty ? catalog : catalog.filter { $0.name.localizedStandardContains(search) }
                List {
                    section("Zuletzt trainiert", matches.filter(\.recent))
                    section("Alle Übungen", matches.filter { !$0.recent })
                }
                .searchable(text: $search, prompt: "Suchen")
            } else {
                ProgressView()
            }
        }
        .navigationTitle("Übung")
        .task { model.loadCatalog() }
    }

    @ViewBuilder
    private func section(_ title: LocalizedStringKey, _ entries: [WatchCatalogEntry]) -> some View {
        if !entries.isEmpty {
            Section(title) {
                ForEach(entries) { entry in
                    Button(entry.name) {
                        model.send(.addExercise(id: entry.id))
                        dismiss()
                    }
                }
            }
        }
    }
}
