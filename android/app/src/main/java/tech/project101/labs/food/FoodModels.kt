package tech.project101.labs.food

import tech.project101.labs.api.models.ExtractedFoodDto
import tech.project101.labs.api.models.FoodDto
import tech.project101.labs.api.models.FoodPortionDto
import tech.project101.labs.api.models.FoodWithLastEntryAndPortionsDto
import tech.project101.labs.api.models.FoodWithPortionsDto
import tech.project101.labs.api.models.SearchFoodResponseDto
import tech.project101.labs.api.models.TrackedFoodDto
import java.time.Instant
import java.time.LocalTime
import kotlin.math.roundToLong

// the DTOs of the backend (generated from api/openapi.json) as the models of the app, like on iOS

enum class Meal(val label: String) {
    // order in which the meals are shown
    BREAKFAST("Frühstück"),
    LUNCH("Mittagessen"),
    SNACK("Snack"),
    DINNER("Abendessen");

    companion object {
        /** The meal that fits the given time of day. */
        fun current(time: LocalTime = LocalTime.now()): Meal = when {
            time.hour < 10 -> BREAKFAST
            time.hour < 15 -> LUNCH
            time.hour < 18 -> SNACK
            else -> DINNER
        }
    }
}

enum class Nutrient { KCAL, PROTEIN, CARBOHYDRATES, FAT }

/** Anything with nutrients per 100 g. */
interface NutrientsPer100g {
    val kcal: Double?
    val protein: Double?
    val carbohydrates: Double?
    val fat: Double?

    fun per100g(nutrient: Nutrient): Double = when (nutrient) {
        Nutrient.KCAL -> kcal
        Nutrient.PROTEIN -> protein
        Nutrient.CARBOHYDRATES -> carbohydrates
        Nutrient.FAT -> fat
    } ?: 0.0

    /** rounded to one decimal, like the web frontend */
    fun amount(nutrient: Nutrient, grams: Double): Double =
        (per100g(nutrient) * grams / 100 * 10).roundToLong() / 10.0
}

data class Food(
    val id: Long,
    val name: String,
    override val kcal: Double?,
    override val protein: Double?,
    override val carbohydrates: Double?,
    override val fat: Double?,
) : NutrientsPer100g {
    constructor(dto: FoodDto) : this(dto.id, dto.name, dto.kcal, dto.protein, dto.carbohydrates, dto.fat)
}

data class FoodPortion(val id: Long, val grams: Double, val label: String, val isDefault: Boolean) {
    constructor(dto: FoodPortionDto) : this(dto.id, dto.grams, dto.label, dto.isDefault)
}

/** A food with its portions, as returned by the import, the extraction and the "last entry" endpoint. */
data class FoodDetails(
    /** null while the food only exists in the open food database */
    val id: Long?,
    val name: String,
    override val kcal: Double?,
    override val protein: Double?,
    override val carbohydrates: Double?,
    override val fat: Double?,
    val portions: List<FoodPortion>,
    val lastAmount: Double? = null,
) : NutrientsPer100g {
    constructor(dto: FoodWithPortionsDto) : this(
        dto.id, dto.name, dto.kcal, dto.protein, dto.carbohydrates, dto.fat, dto.portions.map(::FoodPortion),
    )

    constructor(dto: FoodWithLastEntryAndPortionsDto) : this(
        dto.id, dto.name, dto.kcal, dto.protein, dto.carbohydrates, dto.fat, dto.portions.map(::FoodPortion), dto.lastEntry?.amount,
    )
}

data class TrackedFood(
    val id: Long,
    val food: Food,
    /** grams without portion, otherwise the number of portions */
    val amount: Double,
    val meal: Meal,
    val portion: FoodPortion?,
    val createDate: Instant?,
) {
    val grams: Double get() = amount * (portion?.grams ?: 1.0)

    fun amount(nutrient: Nutrient): Double = food.amount(nutrient, grams)

    companion object {
        /** null for a meal the app doesn't know */
        fun from(dto: TrackedFoodDto): TrackedFood? {
            val meal = Meal.entries.firstOrNull { it.name == dto.meal.type } ?: return null
            return TrackedFood(
                dto.id, Food(dto.food), dto.amount, meal, dto.portion?.let(::FoodPortion), dto.createDate.toInstant(),
            )
        }
    }
}

data class FoodSearchResult(
    /** null while the food only exists in the open food database */
    val foodId: Long?,
    val name: String,
    /** set if the food has to be imported from the open food database before it can be tracked */
    val openFoodId: Long?,
) {
    val key: String get() = foodId?.let { "food-$it" } ?: "open-food-$openFoodId"

    constructor(dto: SearchFoodResponseDto) : this(dto.id, dto.name, dto.openFoodId)
}

data class ExtractedFood(
    val query: String,
    val amount: Double,
    /** as written in the text, e.g. "Tasse", null if none was given */
    val unit: String?,
    val food: FoodDetails?,
    /** set while the food only exists in the open food database */
    val openFoodId: Long?,
) {
    constructor(dto: ExtractedFoodDto) : this(dto.query, dto.amount, dto.unit, dto.food?.let(::FoodDetails), dto.openFoodId)
}

data class Page<T>(val content: List<T>, val last: Boolean, val number: Int)

data class NutritionGoals(val kcal: Double, val protein: Double, val carbohydrates: Double, val fat: Double) {
    fun of(nutrient: Nutrient) = when (nutrient) {
        Nutrient.KCAL -> kcal
        Nutrient.PROTEIN -> protein
        Nutrient.CARBOHYDRATES -> carbohydrates
        Nutrient.FAT -> fat
    }

    companion object {
        val DEFAULT = NutritionGoals(kcal = 3000.0, protein = 165.0, carbohydrates = 360.0, fat = 100.0)
    }
}
