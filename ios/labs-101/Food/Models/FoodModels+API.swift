import Foundation
import LabsAPI

// the DTOs of the backend (generated from api/openapi.json) as the models of the app

nonisolated extension Food {
    init(_ dto: Components.Schemas.FoodDto) {
        self.init(id: Int(dto.id), name: dto.name, kcal: dto.kcal, protein: dto.protein,
                  carbohydrates: dto.carbohydrates, fat: dto.fat)
    }
}

nonisolated extension FoodPortion {
    init(_ dto: Components.Schemas.FoodPortionDto) {
        self.init(id: Int(dto.id), grams: dto.grams, label: dto.label, isDefault: dto.isDefault)
    }
}

nonisolated extension FoodDetails {
    init(_ dto: Components.Schemas.FoodWithPortionsDto) {
        self.init(id: dto.id.map(Int.init), name: dto.name, kcal: dto.kcal, protein: dto.protein,
                  carbohydrates: dto.carbohydrates, fat: dto.fat, portions: dto.portions.map(FoodPortion.init),
                  lastEntry: nil)
    }

    init(_ dto: Components.Schemas.FoodWithLastEntryAndPortionsDto) {
        self.init(id: Int(dto.id), name: dto.name, kcal: dto.kcal, protein: dto.protein,
                  carbohydrates: dto.carbohydrates, fat: dto.fat, portions: dto.portions.map(FoodPortion.init),
                  lastEntry: dto.lastEntry.map { LastEntry(amount: $0.value1.amount) })
    }
}

nonisolated extension TrackedFood {
    init(_ dto: Components.Schemas.TrackedFoodDto) throws {
        guard let meal = Meal(rawValue: dto.meal._type) else { throw APIError.invalidResponse }
        self.init(id: Int(dto.id), food: Food(dto.food), amount: dto.amount, meal: MealInfo(type: meal),
                  portion: dto.portion.map { FoodPortion($0.value1) }, createDate: dto.createDate)
    }
}

nonisolated extension FoodSearchResult {
    init(_ dto: Components.Schemas.SearchFoodResponseDto) {
        self.init(foodID: dto.id.map(Int.init), name: dto.name, openFoodID: dto.openFoodId.map(Int.init))
    }
}

nonisolated extension ExtractedFood {
    init(_ dto: Components.Schemas.ExtractedFoodDto) {
        self.init(query: dto.query, amount: dto.amount, unit: dto.unit, food: dto.food.map { FoodDetails($0.value1) },
                  openFoodId: dto.openFoodId.map(Int.init))
    }
}

nonisolated extension TrackFoodRequest {
    var dto: Components.Schemas.CreateFoodUserDto {
        .init(amount: amount, foodId: foodId.map(Int64.init), meal: meal.rawValue, portionId: portionId.map(Int64.init))
    }
}
