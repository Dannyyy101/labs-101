package tech.project101.labs.food

import tech.project101.labs.api.apis.FoodControllerApi
import tech.project101.labs.api.apis.UserControllerApi
import tech.project101.labs.api.models.CreateFoodPortionDto
import tech.project101.labs.api.models.CreateFoodUserDto
import tech.project101.labs.api.models.ExtractFoodRequestDto
import tech.project101.labs.core.ApiClient
import tech.project101.labs.core.ensureSuccess
import tech.project101.labs.core.result
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** The food endpoints of the backend, for the signed in user. */
class FoodService(api: ApiClient) {
    private val foods = api.create<FoodControllerApi>()
    private val user = api.create<UserControllerApi>()

    // tracked food

    /** The backend groups tracked food by UTC day, so ask for noon of the selected day. */
    suspend fun trackedFood(day: LocalDate): List<TrackedFood> {
        val noonUtc = OffsetDateTime.of(day, LocalTime.NOON, ZoneOffset.UTC)
        return user.userGetTrackedFoodForUser(noonUtc).result().mapNotNull(TrackedFood::from)
    }

    suspend fun trackedFood(id: Long): TrackedFood =
        TrackedFood.from(user.userGetTrackedFoodById(id).result()) ?: throw IllegalStateException("Unbekannte Mahlzeit")

    suspend fun track(foodId: Long, amount: Double, portionId: Long?, meal: Meal) {
        foods.foodTrackFoodForUser(foodId, request(foodId, amount, portionId, meal)).ensureSuccess()
    }

    suspend fun trackOpenFood(openFoodId: Long, amount: Double, portionId: Long?, meal: Meal) {
        foods.foodTrackOpenFood(openFoodId, request(null, amount, portionId, meal)).ensureSuccess()
    }

    suspend fun update(entry: TrackedFood, amount: Double, portionId: Long?, meal: Meal) {
        user.userUpdateTrackedFoodById(entry.id, request(entry.food.id, amount, portionId, meal)).ensureSuccess()
    }

    suspend fun delete(entry: TrackedFood) {
        user.userDeleteTrackedFoodById(entry.id).ensureSuccess()
    }

    // foods

    suspend fun search(name: String, page: Int = 0): Page<FoodSearchResult> {
        val slice = foods.foodSearch(name = name, page = page).result()
        return Page(slice.content.map(::FoodSearchResult), slice.last, slice.number)
    }

    /** The food with its portions and the amount the user tracked last time. */
    suspend fun details(foodId: Long): FoodDetails = FoodDetails(user.userGetLastTracked(foodId).result())

    /** Looks the barcode up and stores the food, if it isn't known yet. */
    suspend fun food(barcode: String): Food = Food(foods.foodGetByBarcode(barcode).result())

    suspend fun extractFoods(text: String): List<ExtractedFood> =
        foods.foodExtract(ExtractFoodRequestDto(text = text)).result().map(::ExtractedFood)

    /** Adds the portion and returns all portions of the food afterwards. */
    suspend fun addPortion(label: String, grams: Double, foodId: Long): List<FoodPortion> {
        foods.foodAddFoodPortions(foodId, CreateFoodPortionDto(grams = grams, isDefault = false, label = label)).ensureSuccess()
        return details(foodId).portions
    }

    /** A food of the open food database, without importing it. */
    suspend fun openFood(openFoodId: Long): FoodDetails = FoodDetails(foods.foodGetOpenFood(openFoodId).result())

    /** Copies a food of the open food database into our foods. */
    suspend fun importOpenFood(openFoodId: Long): FoodDetails = FoodDetails(foods.foodImportOpenFood(openFoodId).result())

    private fun request(foodId: Long?, amount: Double, portionId: Long?, meal: Meal) =
        CreateFoodUserDto(amount = amount, meal = meal.name, foodId = foodId, portionId = portionId)
}
