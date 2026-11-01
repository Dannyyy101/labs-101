import SwiftUI

/// The running workout: sets are checked off while training, the iOS counterpart of /workouts/session.
struct WorkoutSessionView: View {
    let exercises: [Exercise]
    let history: [WorkoutSession]

    @Environment(\.dismiss) private var dismiss
    @Environment(\.scenePhase) private var scenePhase
    @State private var model: WorkoutSessionModel
    @State private var showPicker = false
    @State private var showCardioPicker = false
    @State private var showFinish = false
    @State private var confirmDiscard = false
    @State private var isFinishing = false
    @State private var actionError: String?
    @FocusState private var focusedField: UUID?

    init(session: WorkoutSession, exercises: [Exercise], history: [WorkoutSession], service: WorkoutService) {
        self.exercises = exercises
        self.history = history
        // the same model the Apple Watch changes
        _model = State(initialValue: WatchBridge.shared.model(for: session, service: service))
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach($model.exercises) { $item in
                    if item.cardio != nil {
                        CardioSection(item: $item,
                                      previous: WorkoutStats.lastCardioMinutes(of: item.exerciseId, in: history),
                                      focusedField: $focusedField,
                                      onRemove: { model.exercises.removeAll { $0.id == item.id } })
                    } else {
                        ExerciseSection(item: $item,
                                        exercise: exercises.first { $0.id == item.exerciseId },
                                        previous: WorkoutStats.lastSets(of: item.exerciseId, in: history),
                                        focusedField: $focusedField,
                                        onToggle: { setID in model.toggle(setID, of: item.id) },
                                        onAddSet: { model.addSet(to: item.id) },
                                        onRemove: { model.exercises.removeAll { $0.id == item.id } })
                    }
                }

                if model.exercises.isEmpty {
                    ContentUnavailableView("Leeres Training", systemImage: "dumbbell",
                                           description: Text("Füge die erste Übung hinzu, um Sätze zu erfassen."))
                }

                Section {
                    Button("Übung hinzufügen", systemImage: "plus") { showPicker = true }
                    Button("Cardio hinzufügen", systemImage: "heart.circle") { showCardioPicker = true }
                    Button("Training verwerfen", role: .destructive) { confirmDiscard = true }
                }

                // at the end of the list, so it isn't carried along while scrolling
                Section {
                    Button {
                        focusedField = nil
                        showFinish = true
                    } label: {
                        Text("Training beenden")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.green)
                    .controlSize(.large)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }
            }
            .listSectionSpacing(.compact)
            .navigationTitle($model.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbar }
            .safeAreaInset(edge: .bottom) { bottomBar }
            .sensoryFeedback(.success, trigger: model.restsFinished)
            .sheet(isPresented: $showPicker) {
                ExercisePickerView(exercises: exercises.filter { !$0.isCardio }) { exercise in
                    model.add(exercise, history: history)
                }
            }
            .sheet(isPresented: $showCardioPicker) {
                ExercisePickerView(exercises: exercises.filter(\.isCardio), title: "Cardio hinzufügen") { exercise in
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
            // finished or discarded on the watch
            .onChange(of: model.isClosed) { _, closed in
                if closed { dismiss() }
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
        // only a failed save is worth a hint, the rest happens quietly
        if model.saveState == .failed {
            ToolbarItem(placement: .status) {
                Button("Nicht gespeichert – erneut versuchen") { Task { await model.save() } }
                    .font(.caption)
                    .tint(.red)
            }
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
            HStack(alignment: .firstTextBaseline) {
                Text(model.session.startedAt, style: .timer)
                    .font(.title2.bold())
                    .monospacedDigit()
                Spacer()
                Text("\(model.doneSets.count) / \(model.allSets.count) Sätze")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 4)
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
            TextField("Notiz", text: note, prompt: Text("Notiz, z. B. Sitz auf Stufe 4"), axis: .vertical)
                .font(.subheadline)
                .lineLimit(1...4)
                .focused(focusedField, equals: item.id)
                .listRowBackground(item.note == nil ? nil : Color.yellow.opacity(0.15))

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

    /// an empty note is sent as none
    private var note: Binding<String> {
        Binding {
            item.note ?? ""
        } set: { text in
            item.note = text.isEmpty ? nil : String(text.prefix(500))
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

            NumberField(value: $set.weightKg, unit: "kg", label: "Gewicht Satz \(number)", id: set.id, focusedField: focusedField)
            RepsField(reps: $set.reps) { focusedField.wrappedValue = nil }

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

}

/// A number typed as text: the whole field can be tapped, the value is selected to type over it and
/// every key changes the value right away (a value field only took it over when leaving the field).
private struct NumberField: View {
    @Binding var value: Double
    let unit: String
    let label: String
    let id: UUID
    var focusedField: FocusState<UUID?>.Binding
    var decimals = true

    @State private var text = ""
    @State private var selection: TextSelection?

    private var isFocused: Bool {
        focusedField.wrappedValue == id
    }

    var body: some View {
        HStack(spacing: 3) {
            TextField(unit, text: $text, selection: $selection)
                .keyboardType(decimals ? .decimalPad : .numberPad)
                .multilineTextAlignment(.trailing)
                .focused(focusedField, equals: id)
                .monospacedDigit()
                .accessibilityLabel(label)
            Text(unit)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .setField()
        // the padding and the unit open the keyboard too, not only the digits
        .contentShape(.rect)
        .onTapGesture { focusedField.wrappedValue = id }
        .onAppear { text = formatted(value) }
        // changed elsewhere, e.g. "last time" taken over or on the watch
        .onChange(of: value) { _, newValue in
            if !isFocused { text = formatted(newValue) }
        }
        .onChange(of: text) { _, newText in
            let filtered = String(newText.filter { $0.isNumber || (decimals && ($0 == "," || $0 == ".")) })
            if filtered != newText {
                text = filtered
                return
            }
            value = Double(filtered.replacingOccurrences(of: ",", with: ".")) ?? 0
        }
        .onChange(of: isFocused) { _, focused in
            if focused {
                // after the tap placed the cursor, otherwise it replaces the selection
                Task { @MainActor in
                    selection = TextSelection(range: text.startIndex..<text.endIndex)
                }
            } else {
                text = formatted(value)
            }
        }
    }

    private func formatted(_ value: Double) -> String {
        value.formatted(.number.precision(.fractionLength(0...2)).grouping(.never))
    }
}

/// Cardio like running or rowing: only its minutes, checked off like a set.
private struct CardioSection: View {
    @Binding var item: SessionExercise
    let previous: Int?
    var focusedField: FocusState<UUID?>.Binding
    let onRemove: () -> Void

    @State private var confirmRemove = false

    var body: some View {
        Section {
            if let cardio = Binding($item.cardio) {
                HStack(spacing: 10) {
                    Image(systemName: "heart.fill")
                        .foregroundStyle(.pink)
                        .frame(width: 22)
                    Stepper("Minuten", value: cardio.minutes, in: 0...600, step: 5)
                        .labelsHidden()
                    Spacer()
                    NumberField(value: minutes(cardio), unit: "min", label: "Minuten \(item.name)",
                                id: item.id, focusedField: focusedField, decimals: false)
                    Button(cardio.wrappedValue.done ? "\(item.name) nicht erledigt" : "\(item.name) erledigt", systemImage: "checkmark") {
                        focusedField.wrappedValue = nil
                        cardio.wrappedValue.done.toggle()
                    }
                    .labelStyle(.iconOnly)
                    .font(.headline)
                    .frame(width: 40, height: 34)
                    .foregroundStyle(cardio.wrappedValue.done ? .white : .secondary)
                    .background(cardio.wrappedValue.done ? Color.green : Color(.tertiarySystemFill), in: .rect(cornerRadius: 10))
                    .buttonStyle(.plain)
                    .sensoryFeedback(.impact, trigger: cardio.wrappedValue.done)
                }
                .listRowBackground(cardio.wrappedValue.done ? Color.green.opacity(0.12) : nil)
            }
        } header: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(item.name)
                        if item.cardio?.done == true {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                        }
                    }
                    .font(.headline)
                    .foregroundStyle(.primary)
                    Text(previous.map { "Letztes Mal: \($0) min" } ?? "Cardio")
                        .font(.caption)
                        .textCase(nil)
                        .lineLimit(1)
                }
                Spacer()
                Button("Cardio entfernen", systemImage: "trash", role: .destructive) {
                    if item.cardio?.done == true { confirmRemove = true } else { onRemove() }
                }
                .labelStyle(.iconOnly)
                .confirmationDialog("\(item.name) entfernen?", isPresented: $confirmRemove, titleVisibility: .visible) {
                    Button("Entfernen", role: .destructive, action: onRemove)
                }
            }
        }
    }

    private func minutes(_ cardio: Binding<SessionCardio>) -> Binding<Double> {
        Binding {
            Double(cardio.wrappedValue.minutes)
        } set: { value in
            cardio.wrappedValue.minutes = max(0, Int(value.rounded()))
        }
    }
}

/// The reps are picked from a wheel, typing them into a tiny field was fiddly.
private struct RepsField: View {
    @Binding var reps: Int
    let onOpen: () -> Void

    @State private var isPicking = false

    var body: some View {
        Button {
            onOpen()
            isPicking = true
        } label: {
            HStack(spacing: 3) {
                Text("\(reps)")
                    .monospacedDigit()
                    .frame(maxWidth: .infinity, alignment: .trailing)
                Text("Wdh")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .setField()
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Wiederholungen: \(reps)")
        .popover(isPresented: $isPicking) {
            Picker("Wiederholungen", selection: $reps) {
                ForEach(0...max(50, reps), id: \.self) { value in
                    Text("\(value)").tag(value)
                }
            }
            .pickerStyle(.wheel)
            .frame(width: 140, height: 200)
            .presentationCompactAdaptation(.popover)
        }
    }
}

private extension View {
    func setField() -> some View {
        padding(.horizontal, 8)
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

private struct FinishSheet: View {
    let model: WorkoutSessionModel
    let isFinishing: Bool
    let onFinish: () -> Void
    let onDiscard: () -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let done = model.doneSets
        let cardio = model.exercises.compactMap(\.cardio)
        let cardioDone = cardio.filter { $0.done && $0.minutes > 0 }
        let open = model.allSets.count - done.count + cardio.count - cardioDone.count
        // done cardio alone is worth saving too
        let nothingDone = done.isEmpty && cardioDone.isEmpty
        NavigationStack {
            VStack(spacing: 20) {
                Text(message(done: done.count + cardioDone.count, open: open))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                HStack {
                    stat("Zeit", model.session.duration().workoutDuration)
                    stat("Sätze", "\(done.count)")
                    if done.isEmpty && !cardioDone.isEmpty {
                        stat("Cardio", "\(cardioDone.reduce(0) { $0 + $1.minutes }) min")
                    } else {
                        stat("Volumen", WorkoutStats.formattedVolume(WorkoutStats.volume(done)))
                    }
                }
                .padding()
                .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 14))

                Spacer()

                if nothingDone {
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
            .navigationTitle(nothingDone ? "Noch kein Satz erledigt" : "Training beenden?")
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
            return "Starke Leistung, alles erledigt!"
        }
        return open == 1
            ? "1 Eintrag ist nicht abgehakt und wird nicht gespeichert."
            : "\(open) Einträge sind nicht abgehakt und werden nicht gespeichert."
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
