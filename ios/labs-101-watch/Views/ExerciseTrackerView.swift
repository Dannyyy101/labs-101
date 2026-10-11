import SwiftUI

/// Tracks one exercise: pick a set, set weight and reps with the Digital Crown, check it off, rest.
struct ExerciseTrackerView: View {
    let model: WatchWorkoutModel
    let exerciseID: UUID

    private enum Field: Hashable {
        case weight, reps
    }

    @State private var selectedSetID: UUID?
    @FocusState private var field: Field?

    private var exercise: WatchWorkoutState.Exercise? {
        model.state?.exercises.first { $0.id == exerciseID }
    }

    var body: some View {
        if let exercise {
            Group {
                if model.isResting, let until = model.state?.restUntil {
                    RestView(until: until, total: model.state?.restTotal ?? 90, next: selectedSet(of: exercise),
                             onAdjust: { model.send(.adjustRest(seconds: $0)) }, onEnd: { model.send(.endRest) })
                } else {
                    tracker(exercise)
                }
            }
            .navigationTitle(exercise.name)
            .onAppear { selectFirstOpenSet(of: exercise) }
            .sensoryFeedback(.success, trigger: exercise.sets.filter(\.done).count)
        } else {
            ContentUnavailableView("Übung entfernt", systemImage: "dumbbell")
        }
    }

    @ViewBuilder
    private func tracker(_ exercise: WatchWorkoutState.Exercise) -> some View {
        ScrollView {
            VStack(spacing: 8) {
                SetChips(sets: exercise.sets, selected: selectedSetID) { selectedSetID = $0 }

                if let selected = selectedSet(of: exercise) {
                    let (index, set) = selected
                    HStack(spacing: 6) {
                        valueBox(set.weightKg.weightFormatted, unit: "kg", field: .weight, done: set.done,
                                 value: weightBinding(set), range: 0...500, step: 0.5, sensitivity: .high)
                        valueBox("\(set.reps)", unit: "Wdh", field: .reps, done: set.done,
                                 value: repsBinding(set), range: 0...100, step: 1, sensitivity: .low)
                    }

                    if let previous = exercise.previous?[safe: index] {
                        Button("Letztes Mal \(previous.formatted)") {
                            model.update(exercise: exerciseID, set: set.id, reps: previous.reps, weightKg: previous.weightKg)
                        }
                        .buttonStyle(.plain)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    }

                    Button {
                        toggle(set, in: exercise)
                    } label: {
                        Label(set.done ? "Rückgängig" : "Satz \(index + 1) erledigt",
                              systemImage: set.done ? "arrow.uturn.backward" : "checkmark")
                            .frame(maxWidth: .infinity)
                    }
                    .tint(set.done ? .gray : .green)
                    .buttonStyle(.borderedProminent)
                } else if !exercise.sets.isEmpty {
                    Label("Alle Sätze erledigt", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                }

                Button("Satz hinzufügen", systemImage: "plus") {
                    model.send(.addSet(exercise: exerciseID))
                    // the new set comes with the next state, it is selected once it is there
                    selectedSetID = nil
                }
                .font(.footnote)
            }
        }
        // the Digital Crown changes the weight right away
        .defaultFocus($field, .weight)
        .onChange(of: exercise.sets.map(\.id)) { _, _ in
            if selectedSet(of: exercise) == nil { selectFirstOpenSet(of: exercise) }
        }
    }

    // MARK: Values

    private func valueBox(_ text: String, unit: LocalizedStringKey, field: Field, done: Bool, value: Binding<Double>,
                          range: ClosedRange<Double>, step: Double, sensitivity: DigitalCrownRotationalSensitivity) -> some View {
        let isFocused = self.field == field
        return VStack(spacing: 0) {
            Text(text)
                .font(.system(.title2, design: .rounded).weight(.semibold))
                .monospacedDigit()
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            Text(unit)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .background(done ? Color.green.opacity(0.2) : Color.white.opacity(0.1), in: .rect(cornerRadius: 10))
        .overlay {
            RoundedRectangle(cornerRadius: 10)
                .stroke(isFocused ? Color.green : .clear, lineWidth: 2)
        }
        .focusable()
        .focused($field, equals: field)
        .digitalCrownRotation(value, from: range.lowerBound, through: range.upperBound, by: step,
                              sensitivity: sensitivity, isContinuous: false, isHapticFeedbackEnabled: true)
        .onTapGesture { self.field = field }
        .accessibilityLabel(Text(field == .weight ? "Gewicht" : "Wiederholungen"))
        .accessibilityValue(Text(text))
    }

    private func weightBinding(_ set: WatchWorkoutState.WatchSet) -> Binding<Double> {
        Binding {
            set.weightKg
        } set: { value in
            let rounded = (value * 2).rounded() / 2
            guard rounded != set.weightKg else { return }
            model.update(exercise: exerciseID, set: set.id, reps: set.reps, weightKg: rounded)
        }
    }

    private func repsBinding(_ set: WatchWorkoutState.WatchSet) -> Binding<Double> {
        Binding {
            Double(set.reps)
        } set: { value in
            let reps = Int(value.rounded())
            guard reps != set.reps else { return }
            model.update(exercise: exerciseID, set: set.id, reps: reps, weightKg: set.weightKg)
        }
    }

    // MARK: Sets

    private func selectedSet(of exercise: WatchWorkoutState.Exercise) -> (index: Int, set: WatchWorkoutState.WatchSet)? {
        guard let index = exercise.sets.firstIndex(where: { $0.id == selectedSetID }) else { return nil }
        return (index, exercise.sets[index])
    }

    private func selectFirstOpenSet(of exercise: WatchWorkoutState.Exercise) {
        selectedSetID = exercise.sets.first { !$0.done }?.id
    }

    private func toggle(_ set: WatchWorkoutState.WatchSet, in exercise: WatchWorkoutState.Exercise) {
        model.send(.toggle(exercise: exerciseID, set: set.id))
        guard !set.done else { return }
        // on to the next open set, it is shown below the rest timer
        let after = exercise.sets.drop { $0.id != set.id }.dropFirst()
        selectedSetID = (after.first { !$0.done } ?? exercise.sets.first { !$0.done && $0.id != set.id })?.id
    }
}

/// The sets as numbered dots, done ones filled.
private struct SetChips: View {
    let sets: [WatchWorkoutState.WatchSet]
    let selected: UUID?
    let onSelect: (UUID) -> Void

    var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 5) {
                ForEach(Array(sets.enumerated()), id: \.element.id) { index, set in
                    Button {
                        onSelect(set.id)
                    } label: {
                        Text("\(index + 1)")
                            .font(.footnote.weight(.semibold))
                            .frame(width: 26, height: 26)
                            .foregroundStyle(set.done ? .black : .primary)
                            .background(set.done ? Color.green : Color.white.opacity(0.15), in: .circle)
                            .overlay {
                                Circle().stroke(set.id == selected ? Color.white : .clear, lineWidth: 2)
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("Satz \(index + 1)\(set.done ? ", erledigt" : "")"))
                }
            }
        }
        .scrollIndicators(.hidden)
    }
}

/// The countdown after a set, with the next set below.
private struct RestView: View {
    let until: Date
    let total: TimeInterval
    let next: (index: Int, set: WatchWorkoutState.WatchSet)?
    let onAdjust: (TimeInterval) -> Void
    let onEnd: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let left = max(0, until.timeIntervalSince(context.date))
                Gauge(value: left, in: 0...max(total, left)) {
                    Text("Pause")
                } currentValueLabel: {
                    Text(left.rounded(.up).workoutDuration)
                        .monospacedDigit()
                }
                .gaugeStyle(.accessoryCircularCapacity)
                .tint(.orange)
                .scaleEffect(1.3)
                .frame(height: 76)
            }

            if let next {
                Text("Als Nächstes: Satz \(next.index + 1) · \(next.set.formatted)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }

            HStack {
                Button("−15") { onAdjust(-15) }
                Button("+15") { onAdjust(15) }
                Button("Pause beenden", systemImage: "forward.fill", action: onEnd)
                    .labelStyle(.iconOnly)
                    .tint(.orange)
            }
            .controlSize(.small)
        }
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
