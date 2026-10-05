import Foundation
import SwiftUI

nonisolated enum Meal: String, Codable, CaseIterable, Identifiable, Sendable {
    // order in which the meals are shown
    case breakfast = "BREAKFAST"
    case lunch = "LUNCH"
    case snack = "SNACK"
    case dinner = "DINNER"

    var id: Self { self }

    var label: LocalizedStringResource {
        switch self {
        case .breakfast: "Frühstück"
        case .lunch: "Mittagessen"
        case .snack: "Snack"
        case .dinner: "Abendessen"
        }
    }

    var color: Color {
        switch self {
        case .breakfast: .orange
        case .lunch: .blue
        case .snack: .green
        case .dinner: .red
        }
    }

    /// The meal that fits the given time of day.
    static func current(at date: Date = .now, calendar: Calendar = .current) -> Meal {
        switch calendar.component(.hour, from: date) {
        case ..<10: .breakfast
        case ..<15: .lunch
        case ..<18: .snack
        default: .dinner
        }
    }
}

nonisolated enum Nutrient: CaseIterable, Sendable {
    case kcal, protein, carbohydrates, fat
}

/// Anything with nutrients per 100 g.
nonisolated protocol NutrientsPer100g {
    var kcal: Double? { get }
    var protein: Double? { get }
    var carbohydrates: Double? { get }
    var fat: Double? { get }
}

nonisolated extension NutrientsPer100g {
    func per100g(_ nutrient: Nutrient) -> Double {
        switch nutrient {
        case .kcal: kcal ?? 0
        case .protein: protein ?? 0
        case .carbohydrates: carbohydrates ?? 0
        case .fat: fat ?? 0
        }
    }

    /// Rounded to one decimal, like the web frontend.
    func amount(of nutrient: Nutrient, grams: Double) -> Double {
        (per100g(nutrient) * grams / 100 * 10).rounded() / 10
    }
}

nonisolated struct Food: Codable, Hashable, Identifiable, Sendable, NutrientsPer100g {
    let id: Int
    let name: String
    let kcal: Double?
    let protein: Double?
    let carbohydrates: Double?
    let fat: Double?
}

nonisolated struct FoodPortion: Codable, Hashable, Identifiable, Sendable {
    let id: Int
    let grams: Double
    let label: String
    let isDefault: Bool
}

/// A food with its portions, as returned by the import, the extraction and the "last entry" endpoint.
nonisolated struct FoodDetails: Codable, Hashable, Sendable, NutrientsPer100g {
    nonisolated struct LastEntry: Codable, Hashable, Sendable {
        let amount: Double
    }

    /// nil while the food only exists in the open food database
    let id: Int?
    let name: String
    let kcal: Double?
    let protein: Double?
    let carbohydrates: Double?
    let fat: Double?
    var portions: [FoodPortion]
    var lastEntry: LastEntry?
}

nonisolated struct TrackedFood: Codable, Hashable, Identifiable, Sendable {
    nonisolated struct MealInfo: Codable, Hashable, Sendable {
        let type: Meal
    }

    let id: Int
    let food: Food
    /// grams without portion, otherwise the number of portions
    let amount: Double
    let meal: MealInfo
    let portion: FoodPortion?
    let createDate: Date?

    var grams: Double {
        amount * (portion?.grams ?? 1)
    }

    func amount(of nutrient: Nutrient) -> Double {
        food.amount(of: nutrient, grams: grams)
    }
}

/// Body of the track/update endpoints.
nonisolated struct TrackFoodRequest: Encodable, Sendable {
    var foodId: Int?
    var userId: String
    var amount: Double
    var meal: Meal
    var portionId: Int?
}

nonisolated struct NewFoodPortion: Encodable, Sendable {
    let label: String
    let grams: Double
    let isDefault: Bool
}

nonisolated struct FoodSearchResult: Codable, Hashable, Identifiable, Sendable {
    /// nil while the food only exists in the open food database
    let foodID: Int?
    let name: String
    /// set if the food has to be imported from the open food database before it can be tracked
    let openFoodID: Int?

    var id: String {
        if let foodID { "food-\(foodID)" } else { "open-food-\(openFoodID ?? 0)" }
    }

    enum CodingKeys: String, CodingKey {
        case foodID = "id"
        case name
        case openFoodID = "openFoodId"
    }
}

nonisolated struct ExtractedFood: Codable, Sendable {
    let query: String
    let amount: Double
    /// as written in the text, e.g. "Tasse", nil if none was given
    let unit: String?
    let food: FoodDetails?
    /// set while the food only exists in the open food database
    let openFoodId: Int?
}

nonisolated struct Page<Element: Decodable & Sendable>: Decodable, Sendable {
    let content: [Element]
    let last: Bool
    let number: Int
}

nonisolated struct NutritionGoals: Sendable {
    let kcal: Double
    let protein: Double
    let carbohydrates: Double
    let fat: Double

    static let `default` = NutritionGoals(kcal: 3000, protein: 165, carbohydrates: 360, fat: 100)
}
