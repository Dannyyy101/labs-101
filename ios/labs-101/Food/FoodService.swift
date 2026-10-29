import Foundation
import LabsAPI
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
        let noonUTC = DateComponents(calendar: Calendar(identifier: .gregorian), timeZone: .gmt, year: components.year,
                                     month: components.month, day: components.day, hour: 12).date ?? day
        return try await client.call { try await $0.userGetTrackedFoodForUser(query: .init(date: noonUTC)).ok.body.json }
            .map(TrackedFood.init)
    }

    func track(foodID: Int, amount: Double, portionID: Int?, meal: Meal) async throws {
        let body = request(foodID: foodID, amount: amount, portionID: portionID, meal: meal).dto
        _ = try await client.call {
            try await $0.foodTrackFoodForUser(path: .init(id: Int64(foodID)), body: .json(body)).noContent
        }
    }

    func trackOpenFood(_ openFoodID: Int, amount: Double, portionID: Int?, meal: Meal) async throws {
        let body = request(foodID: nil, amount: amount, portionID: portionID, meal: meal).dto
        _ = try await client.call {
            try await $0.foodTrackOpenFood(path: .init(openFoodId: Int64(openFoodID)), body: .json(body)).noContent
        }
    }

    func update(_ trackedFood: TrackedFood, amount: Double, portionID: Int?, meal: Meal) async throws {
        let body = request(foodID: trackedFood.food.id, amount: amount, portionID: portionID, meal: meal).dto
        _ = try await client.call {
            try await $0.userUpdateTrackedFoodById(path: .init(trackedFoodId: Int64(trackedFood.id)), body: .json(body)).ok
        }
    }

    func delete(_ trackedFood: TrackedFood) async throws {
        _ = try await client.call {
            try await $0.userDeleteTrackedFoodById(path: .init(trackedFoodId: Int64(trackedFood.id))).ok
        }
    }

    // MARK: Foods

    func search(_ name: String, page: Int = 0) async throws -> Page<FoodSearchResult> {
        let slice = try await client.call {
            try await $0.foodSearch(query: .init(page: page, name: name)).ok.body.json
        }
        return Page(content: slice.content.map(FoodSearchResult.init), last: slice.last, number: Int(slice.number))
    }

    /// The food with its portions and the amount the user tracked last time.
    func details(foodID: Int) async throws -> FoodDetails {
        FoodDetails(try await client.call { try await $0.userGetLastTracked(path: .init(foodId: Int64(foodID))).ok.body.json })
    }

    /// Looks the barcode up and stores the food, if it isn't known yet.
    func food(barcode: String) async throws -> Food {
        Food(try await client.call { try await $0.foodGetByBarcode(path: .init(code: barcode)).ok.body.json })
    }

    func extractFoods(from text: String) async throws -> [ExtractedFood] {
        try await client.call { try await $0.foodExtract(body: .json(.init(text: text))).ok.body.json }
            .map(ExtractedFood.init)
    }

    /// Adds the portion and returns all portions of the food afterwards.
    func addPortion(_ portion: NewFoodPortion, toFood foodID: Int) async throws -> [FoodPortion] {
        let body = Components.Schemas.CreateFoodPortionDto(grams: portion.grams, isDefault: portion.isDefault, label: portion.label)
        _ = try await client.call {
            try await $0.foodAddFoodPortions(path: .init(id: Int64(foodID)), body: .json(body)).noContent
        }
        return try await details(foodID: foodID).portions
    }

    /// A food of the open food database, without importing it.
    func openFood(_ openFoodID: Int) async throws -> FoodDetails {
        FoodDetails(try await client.call {
            try await $0.foodGetOpenFood(path: .init(openFoodId: Int64(openFoodID))).ok.body.json
        })
    }

    /// Copies a food of the open food database into our foods.
    func importOpenFood(_ openFoodID: Int) async throws -> FoodDetails {
        FoodDetails(try await client.call {
            try await $0.foodImportOpenFood(path: .init(openFoodId: Int64(openFoodID))).ok.body.json
        })
    }

    private func request(foodID: Int?, amount: Double, portionID: Int?, meal: Meal) -> TrackFoodRequest {
        TrackFoodRequest(foodId: foodID, amount: amount, meal: meal, portionId: portionID)
    }
}

extension EnvironmentValues {
    @Entry var foodService = FoodService()
}
