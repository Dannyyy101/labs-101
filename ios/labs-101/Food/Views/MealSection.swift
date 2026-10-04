import SwiftUI

/// One meal of the day with its tracked food.
struct MealSection: View {
    let meal: Meal
    let entries: [TrackedFood]
    let remainingKcal: Double
    let onAdd: () -> Void
    let onSelect: (TrackedFood) -> Void
    let onDelete: (TrackedFood) -> Void

    var body: some View {
        Section {
            if entries.isEmpty {
                Text("Noch nichts eingetragen. \(max(remainingKcal, 0).roundedFormatted) kcal stehen dir noch zur Verfügung.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            ForEach(entries) { entry in
                Button {
                    onSelect(entry)
                } label: {
                    TrackedFoodRow(entry: entry)
                }
                .tint(.primary)
                .swipeActions {
                    Button("Entfernen", systemImage: "trash", role: .destructive) {
                        onDelete(entry)
                    }
                }
            }

            Button("Hinzufügen", systemImage: "plus.circle.fill", action: onAdd)
        } header: {
            MealHeader(meal: meal, entries: entries)
        }
    }
}

private struct MealHeader: View {
    let meal: Meal
    let entries: [TrackedFood]

    private var kcal: Double {
        entries.reduce(0) { $0 + $1.amount(of: .kcal) }
    }

    private var firstEntry: Date? {
        entries.compactMap(\.createDate).min()
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Circle()
                .fill(meal.color)
                .frame(width: 10, height: 10)
                .accessibilityHidden(true)
            Text(meal.label)
                .font(.headline)
                .foregroundStyle(.primary)
            if let firstEntry {
                Text(firstEntry, format: .dateTime.hour().minute())
                    .monospacedDigit()
            }
            Spacer()
            Text("\(kcal.roundedFormatted) kcal")
                .monospacedDigit()
        }
        .textCase(nil)
    }
}

struct TrackedFoodRow: View {
    let entry: TrackedFood

    private var amount: String {
        if let portion = entry.portion {
            "\(entry.amount.compactFormatted) \(portion.label) · \(entry.grams.roundedFormatted) g"
        } else {
            "\(entry.amount.roundedFormatted) g"
        }
    }

    private var macros: String {
        "P \(entry.amount(of: .protein).roundedFormatted) · K \(entry.amount(of: .carbohydrates).roundedFormatted) · F \(entry.amount(of: .fat).roundedFormatted)"
    }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.food.name)
                    .lineLimit(1)
                Text("\(amount) · \(macros)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
            Spacer()
            Text(entry.amount(of: .kcal).roundedFormatted)
                .foregroundStyle(.secondary)
                .monospacedDigit()
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}
