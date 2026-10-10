import SwiftUI

/// The running workout: sets are checked off while training, the iOS counterpart of /workouts/session.
struct WorkoutSessionView: View {
    let exercises: [Exercise]
    let history: [WorkoutSession]

    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase
    @State private var model: WorkoutSessionModel
    @State private var showPicker = false
    @State private var showFinish = false
    @State private var confirmDiscard = false
    @State private var isFinishing = false
    @State private var actionError: String?
    @FocusState private var focusedField: UUID?

    init(session: WorkoutSession, exercises: [Exercise], history: [WorkoutSession], service: WorkoutService) {
        self.exercises = exercises
        self.history = history
        _model = State(initialValue: WorkoutSessionModel(session: session, service: service))
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach($model.exercises) { $item in
                    ExerciseSection(item: $item,
                                    exercise: exercises.first { $0.id == item.exerciseId },
                                    previous: WorkoutStats.lastSets(of: item.exerciseId, in: history),
                                    focusedField: $focusedField,
                                    onToggle: { setID in model.toggle(setID, of: item.id) },
                                    onAddSet: { model.addSet(to: item.id) },
                                    onRemove: { model.exercises.removeAll { $0.id == item.id } })
                }

                if model.exercises.isEmpty {
                    ContentUnavailableView("Leeres Training", systemImage: "dumbbell",
                                           description: Text("Füge die erste Übung hinzu, um Sätze zu erfassen."))
                }

                Section {
                    Button("Übung hinzufügen", systemImage: "plus") { showPicker = true }
                    Button("Training verwerfen", role: .destructive) { confirmDiscard = true }
                }
            }
            .listSectionSpacing(.compact)
            .navigationTitle($model.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbar }
            .safeAreaInset(edge: .bottom) { bottomBar }
            .sensoryFeedback(.success, trigger: model.restsFinished)
            .sheet(isPresented: $showPicker) {
                ExercisePickerView(exercises: exercises) { exercise in
                    model.add(exercise, history: history)
                }
            }
            .sheet(isPresented: $showFinish) {
                FinishSheet(model: model, isFinishing: isFinishing, onFinish: finish, onDiscard: discard)
                    .presentationDetents([.medium])
            }
            .confirmationDialog("Training verwerfen?", isPresented: $confirmDiscard, titleVisibility: .visible) {
                Button("Verwerfen", role: .destructive) { discard() }
            } message: {
                Text("Alle erfassten Sätze gehen verloren.")
            }
            .alert("Fehler", isPresented: $actionError.isPresent) {} message: {
                Text(actionError ?? "")
            }
            .onChange(of: scenePhase) { _, phase in
                if phase != .active { model.saveNow() }
            }
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button("Schließen", systemImage: "chevron.down") {
                model.saveNow()
                dismiss()
            }
        }
        ToolbarItem(placement: .status) {
            SaveStatus(state: model.saveState) { Task { await model.save() } }
        }
        ToolbarItemGroup(placement: .keyboard) {
            Spacer()
            Button("Fertig") { focusedField = nil }
        }
    }

    private var bottomBar: some View {
        VStack(spacing: 8) {
            if let rest = model.rest {
                RestBar(rest: rest, onAdjust: model.adjustRest, onEnd: model.endRest)
            }
            HStack {
                VStack(alignment: .leading) {
                    Text(model.session.startedAt, style: .timer)
                        .font(.title2.bold())
                        .monospacedDigit()
                    Text("\(model.doneSets.count) / \(model.allSets.count) Sätze")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button("Beenden") {
                    focusedField = nil
                    showFinish = true
                }
                .buttonStyle(.glassProminent)
                .tint(.green)
                .controlSize(.large)
            }
        }
        .padding(12)
        .glassEffect(in: .rect(cornerRadius: 24))
        .padding(.horizontal)
    }

    private func finish() {
        isFinishing = true
        Task {
            defer { isFinishing = false }
            do {
                try await model.finish()
                showFinish = false
                dismiss()
            } catch {
                actionError = error.localizedDescription
            }
        }
    }

    private func discard() {
        Task {
            do {
                try await model.discard()
                showFinish = false
                dismiss()
            } catch {
                actionError = error.localizedDescription
            }
        }
    }
}

// MARK: Exercises

private struct ExerciseSection: View {
    @Binding var item: SessionExercise
    let exercise: Exercise?
    let previous: [SessionSet]?
    var focusedField: FocusState<UUID?>.Binding
    let onToggle: (SessionSet.ID) -> Void
    let onAddSet: () -> Void
    let onRemove: () -> Void

    @State private var confirmRemove = false

    var body: some View {
        Section {
            ForEach(Array($item.sets.enumerated()), id: \.element.id) { index, $set in
                SetRow(number: index + 1, set: $set, previous: previous?[safe: index],
                       focusedField: focusedField) { onToggle(set.id) }
            }
            .onDelete { item.sets.remove(atOffsets: $0) }

            Button("Satz hinzufügen", systemImage: "plus", action: onAddSet)
                .font(.subheadline)
        } header: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(item.name)
                        if !item.sets.isEmpty && item.sets.allSatisfy(\.done) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                        }
                    }
                    .font(.headline)
                    .foregroundStyle(.primary)
                    Text(subtitle)
                        .font(.caption)
                        .textCase(nil)
                        .lineLimit(1)
                }
                Spacer()
                Button("Übung entfernen", systemImage: "trash", role: .destructive) {
                    if item.sets.contains(where: \.done) { confirmRemove = true } else { onRemove() }
                }
                .labelStyle(.iconOnly)
                .confirmationDialog("\(item.name) mit allen Sätzen entfernen?", isPresented: $confirmRemove, titleVisibility: .visible) {
                    Button("Entfernen", role: .destructive, action: onRemove)
                }
            }
        }
    }

    private var subtitle: String {
        if let previous {
            return "Letztes Mal: " + previous.map(\.formatted).joined(separator: ", ")
        }
        let muscles = exercise?.muscles ?? []
        return muscles.isEmpty ? "Zum ersten Mal" : muscles.joined(separator: ", ")
    }
}

private struct SetRow: View {
    let number: Int
    @Binding var set: SessionSet
    let previous: SessionSet?
    var focusedField: FocusState<UUID?>.Binding
    let onToggle: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            setLine
            restPicker
        }
        .listRowBackground(set.done ? Color.green.opacity(0.12) : nil)
    }

    private var setLine: some View {
        HStack(spacing: 10) {
            Text("\(number)")
                .font(.headline)
                .foregroundStyle(set.done ? .green : .secondary)
                .frame(width: 22)

            Button {
                if let previous {
                    set.weightKg = previous.weightKg
                    set.reps = previous.reps
                }
            } label: {
                Text(previous.map { "\($0.weightKg.compactFormatted) × \($0.reps)" } ?? "–")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)
            .disabled(previous == nil)

            field(value: $set.weightKg, unit: "kg", keyboard: .decimalPad, focus: set.id)
            field(value: Binding { Double(set.reps) } set: { set.reps = max(0, Int($0.rounded())) },
                  unit: "Wdh", keyboard: .numberPad, focus: repsFocus)

            Button(set.done ? "Satz \(number) nicht erledigt" : "Satz \(number) erledigt", systemImage: "checkmark") {
                focusedField.wrappedValue = nil
                onToggle()
            }
            .labelStyle(.iconOnly)
            .font(.headline)
            .frame(width: 40, height: 34)
            .foregroundStyle(set.done ? .white : .secondary)
            .background(set.done ? Color.green : Color(.tertiarySystemFill), in: .rect(cornerRadius: 10))
            .buttonStyle(.plain)
            .sensoryFeedback(.impact, trigger: set.done)
        }
    }

    /// the rest after the set, shown between the sets
    private var restPicker: some View {
        let options = WorkoutStats.restOptions.contains(set.rest) ? WorkoutStats.restOptions : (WorkoutStats.restOptions + [set.rest]).sorted()
        return Menu {
            Picker("Ruhezeit nach dem Satz", selection: $set.rest) {
                ForEach(options, id: \.self) { seconds in
                    Text(WorkoutStats.formattedRest(seconds)).tag(seconds)
                }
            }
            .pickerStyle(.inline)
        } label: {
            HStack(spacing: 8) {
                Capsule().fill(.quaternary).frame(height: 1)
                Label(WorkoutStats.formattedRest(set.rest), systemImage: "timer")
                    .font(.caption.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(set.rest > 0 ? .orange : .secondary)
                Capsule().fill(.quaternary).frame(height: 1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Ruhezeit nach Satz \(number): \(WorkoutStats.formattedRest(set.rest))")
    }

    /// the weight field uses the id of the set, the reps field a derived one
    private var repsFocus: UUID {
        var bytes = set.id.uuid
        bytes.15 ^= 0xFF
        return UUID(uuid: bytes)
    }

    private func field(value: Binding<Double>, unit: LocalizedStringKey, keyboard: UIKeyboardType, focus: UUID) -> some View {
        HStack(spacing: 3) {
            TextField(unit, value: value, format: .number.precision(.fractionLength(0...2)))
                .keyboardType(keyboard)
                .multilineTextAlignment(.trailing)
                .focused(focusedField, equals: focus)
                .monospacedDigit()
            Text(unit)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .frame(width: 84)
        .background(Color(.tertiarySystemFill), in: .rect(cornerRadius: 8))
    }
}

// MARK: Bars and sheets

private struct RestBar: View {
    let rest: WorkoutSessionModel.Rest
    let onAdjust: (TimeInterval) -> Void
    let onEnd: () -> Void

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let left = max(0, rest.until.timeIntervalSince(context.date))
            HStack(spacing: 8) {
                if left > 0 {
                    Text("Pause \(left.rounded(.up).workoutDuration)")
                        .font(.headline)
                        .monospacedDigit()
                        .foregroundStyle(.orange)
                    Spacer()
                    Button("−15") { onAdjust(-15) }
                    Button("+15") { onAdjust(15) }
                } else {
                    Text("Pause vorbei – nächster Satz!")
                        .font(.headline)
                        .foregroundStyle(.green)
                    Spacer()
                }
                Button("Pause beenden", systemImage: "xmark", action: onEnd)
                    .labelStyle(.iconOnly)
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
            .controlSize(.small)
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(alignment: .leading) {
                GeometryReader { geometry in
                    (left > 0 ? Color.orange : Color.green).opacity(0.15)
                        .frame(width: left > 0 ? geometry.size.width * left / rest.total : geometry.size.width)
                        .animation(.linear(duration: 1), value: left)
                }
            }
            .clipShape(.rect(cornerRadius: 14))
        }
    }
}

private struct SaveStatus: View {
    let state: WorkoutSessionModel.SaveState
    let onRetry: () -> Void

    var body: some View {
        switch state {
        case .saved:
            Text("Gespeichert").foregroundStyle(.secondary).font(.caption)
        case .pending, .saving:
            Text("Speichert…").foregroundStyle(.secondary).font(.caption)
        case .failed:
            Button("Nicht gespeichert – erneut versuchen", action: onRetry)
                .font(.caption)
                .tint(.red)
        }
    }
}

private struct FinishSheet: View {
    let model: WorkoutSessionModel
    let isFinishing: Bool
    let onFinish: () -> Void
    let onDiscard: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let done = model.doneSets
        let open = model.allSets.count - done.count
        NavigationStack {
            VStack(spacing: 20) {
                Text(message(done: done.count, open: open))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                HStack {
                    stat("Zeit", model.session.duration().workoutDuration)
                    stat("Sätze", "\(done.count)")
                    stat("Volumen", WorkoutStats.formattedVolume(WorkoutStats.volume(done)))
                }
                .padding()
                .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 14))

                Spacer()

                if done.isEmpty {
                    Button("Training verwerfen", role: .destructive, action: onDiscard)
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                } else {
                    Button(action: onFinish) {
                        Text(isFinishing ? "Speichert…" : "Training beenden")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.green)
                    .controlSize(.large)
                    .disabled(isFinishing)
                }
            }
            .padding()
            .navigationTitle(done.isEmpty ? "Noch kein Satz erledigt" : "Training beenden?")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Weiter trainieren") { dismiss() }
                }
            }
        }
    }

    private func message(done: Int, open: Int) -> String {
        if done == 0 {
            return "Hake erledigte Sätze ab, damit sie gespeichert werden – oder verwirf das Training."
        }
        if open == 0 {
            return "Starke Leistung, alle Sätze erledigt!"
        }
        return open == 1
            ? "1 Satz ist nicht abgehakt und wird nicht gespeichert."
            : "\(open) Sätze sind nicht abgehakt und werden nicht gespeichert."
    }

    private func stat(_ title: LocalizedStringKey, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.headline)
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity)
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
