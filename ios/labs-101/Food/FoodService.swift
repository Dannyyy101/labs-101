import Foundation
import SwiftUI

/// The food endpoints of the backend, for the signed in user.
nonisolated struct FoodService: Sendable {
    private let client: APIClient

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
    }

    // MARK: Tracked food

    /// The backend groups tracked food by UTC day, so ask for noon of the selected day.
    func trackedFood(on day: Date, calendar: Calendar = .current) async throws -> [TrackedFood] {
        let components = calendar.dateComponents([.year, .month, .day], from: day)
        let noonUTC = String(format: "%04d-%02d-%02dT12:00:00Z", components.year ?? 0, components.month ?? 0, components.day ?? 0)
        return try await client.get("users/me/tracked-foods", query: [URLQueryItem(name: "date", value: noonUTC)])
    }

    func track(foodID: Int, amount: Double, portionID: Int?, meal: Meal) async throws {
        let body = request(foodID: foodID, amount: amount, portionID: portionID, meal: meal)
        try await client.send("POST", "foods/\(foodID)/track", body: body)
    }

    func trackOpenFood(_ openFoodID: Int, amount: Double, portionID: Int?, meal: Meal) async throws {
        let body = request(foodID: nil, amount: amount, portionID: portionID, meal: meal)
        try await client.send("POST", "foods/open-food/\(openFoodID)/track", body: body)
    }

    func update(_ trackedFood: TrackedFood, amount: Double, portionID: Int?, meal: Meal) async throws {
        let body = request(foodID: trackedFood.food.id, amount: amount, portionID: portionID, meal: meal)
        try await client.send("PUT", "users/me/tracked-foods/\(trackedFood.id)", body: body)
    }

    func delete(_ trackedFood: TrackedFood) async throws {
        try await client.send("DELETE", "users/me/tracked-foods/\(trackedFood.id)")
    }

    // MARK: Foods

    func search(_ name: String, page: Int = 0) async throws -> Page<FoodSearchResult> {
        try await client.get("foods/search/byNameAndUser", query: [
            URLQueryItem(name: "name", value: name),
            URLQueryItem(name: "page", value: String(page)),
        ])
    }

    /// The food with its portions and the amount the user tracked last time.
    func details(foodID: Int) async throws -> FoodDetails {
        try await client.get("users/me/foods/\(foodID)/last")
    }

    /// Looks the barcode up and stores the food, if it isn't known yet.
    func food(barcode: String) async throws -> Food {
        try await client.get("foods/bar-code/\(barcode)")
    }

    func extractFoods(from text: String) async throws -> [ExtractedFood] {
        try await client.send("POST", "foods/extract", body: ["text": text])
    }

    /// Adds the portion and returns all portions of the food afterwards.
    func addPortion(_ portion: NewFoodPortion, toFood foodID: Int) async throws -> [FoodPortion] {
        try await client.send("POST", "foods/\(foodID)/portions", body: portion)
        return try await details(foodID: foodID).portions
    }

    /// A food of the open food database, without importing it.
    func openFood(_ openFoodID: Int) async throws -> FoodDetails {
        try await client.get("foods/open-food/\(openFoodID)")
    }

    /// Copies a food of the open food database into our foods.
    func importOpenFood(_ openFoodID: Int) async throws -> FoodDetails {
        try await client.send("POST", "foods/open-food/\(openFoodID)/import")
    }

    private func request(foodID: Int?, amount: Double, portionID: Int?, meal: Meal) -> TrackFoodRequest {
        TrackFoodRequest(foodId: foodID, amount: amount, meal: meal, portionId: portionID)
    }
}

extension EnvironmentValues {
    @Entry var foodService = FoodService()
}
