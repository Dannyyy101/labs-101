import SwiftUI

/// Calories left and the macros of the day.
struct DaySummaryView: View {
    let consumed: (Nutrient) -> Double
    let goals: NutritionGoals
    var burnedKcal: Double = 0

    var body: some View {
        VStack(spacing: 20) {
            CalorieRingView(consumed: consumed(.kcal), goal: goals.kcal, burned: burnedKcal)

            Divider()

            VStack(spacing: 14) {
                MacroProgressView(title: "Protein", consumed: consumed(.protein), goal: goals.protein, tint: .protein)
                MacroProgressView(title: "Kohlenhydrate", consumed: consumed(.carbohydrates), goal: goals.carbohydrates, tint: .carbohydrates)
                MacroProgressView(title: "Fett", consumed: consumed(.fat), goal: goals.fat, tint: .fat)
            }
        }
        .padding(.vertical, 8)
    }
}

struct CalorieRingView: View {
    let consumed: Double
    let goal: Double
    let burned: Double

    private var remaining: Double { goal - consumed + burned }
    private var progress: Double {
        let max = goal + burned
        return max > 0 ? min(consumed / max, 1) : 0
    }

    var body: some View {
        HStack(spacing: 24) {
            ZStack {
                Circle()
                    .stroke(.quaternary, lineWidth: 14)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(remaining < 0 ? Color.red : Color.orange, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.smooth, value: progress)
                VStack(spacing: 0) {
                    Text(remaining.roundedFormatted)
                        .font(.title.bold())
                        .monospacedDigit()
                        .contentTransition(.numericText(value: remaining))
                    Text("kcal übrig")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 140, height: 140)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(remaining.roundedFormatted) kcal übrig")
            .accessibilityValue(progress.formatted(.percent.precision(.fractionLength(0))))

            VStack(alignment: .leading, spacing: 8) {
                stat("Ziel", goal.roundedFormatted)
                stat("Gegessen", consumed.roundedFormatted)
                stat("Verbrannt", "+\(burned.roundedFormatted)")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func stat(_ title: LocalizedStringKey, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.title2.weight(.semibold))
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
    }
}

struct MacroProgressView: View {
    let title: LocalizedStringKey
    let consumed: Double
    let goal: Double
    let tint: Color

    var body: some View {
        VStack(spacing: 6) {
            HStack(alignment: .firstTextBaseline) {
                Text(title)
                    .fontWeight(.semibold)
                Spacer()
                Text("\(Text("\(consumed.roundedFormatted) g").fontWeight(.semibold).foregroundStyle(.primary)) / \(goal.roundedFormatted) g")
                    .foregroundStyle(.secondary)
            }
            .monospacedDigit()

            ProgressView(value: min(consumed, goal), total: max(goal, 1))
                .tint(tint)
                .animation(.smooth, value: consumed)
        }
        .accessibilityElement(children: .combine)
    }
}

extension Color {
    static let protein = Color.pink
    static let carbohydrates = Color.blue
    static let fat = Color.green
}

#Preview {
    List {
        DaySummaryView(consumed: { [.kcal: 1960, .protein: 120, .carbohydrates: 210, .fat: 60][$0] ?? 0 }, goals: .default)
    }
}
