import SwiftUI

/// Picks an exercise for the running workout, searchable by name and muscle.
struct ExercisePickerView: View {
    let exercises: [Exercise]
    let onPick: (Exercise) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    private var filtered: [Exercise] {
        let query = query.trimmingCharacters(in: .whitespaces)
        let sorted = exercises.sorted { $0.name.localizedCompare($1.name) == .orderedAscending }
        guard !query.isEmpty else { return sorted }
        return sorted.filter { exercise in
            exercise.name.localizedStandardContains(query)
                || exercise.muscles.contains { $0.localizedStandardContains(query) }
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered) { exercise in
                Button {
                    onPick(exercise)
                    dismiss()
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(exercise.name)
                            .foregroundStyle(.primary)
                        if !exercise.muscles.isEmpty {
                            Text(exercise.muscles.joined(separator: ", "))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .overlay {
                if filtered.isEmpty {
                    ContentUnavailableView.search(text: query)
                }
            }
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Übung suchen…")
            .autocorrectionDisabled()
            .navigationTitle("Übung hinzufügen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) { dismiss() }
                }
            }
        }
    }
}
