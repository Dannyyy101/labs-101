'use client'
import NutritionCard from "./NutritionCard"
import MealView, { MEALS } from "./MealView"
import { TrackedFood, Meal } from "@/utils/types/food";
import { getNutritionForAmount } from "@/utils/food";
import CalorieCard from "./CalorieCard";
import { SelectedFoodAction, useSelectedFoodStore } from "@/lib/zustand/selectedFood";
import FoodTrackView from "./FoodTrackView";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import FoodSearch, { MealTabs } from "./FoodSearch";
import { Plus } from "lucide-react";
import { formatDayTitle, formatLongDay } from "./format";

const CALORIE_GOAL = 3000
const NUTRITION_GOALS = { protein: 165, carbohydrates: 360, fat: 100 }

// meal that fits the current time of day
const currentMeal = () => {
    const hour = new Date().getHours()
    if (hour < 10) return Meal.BREAKFAST
    if (hour < 15) return Meal.LUNCH
    if (hour < 18) return Meal.SNACK
    return Meal.DINNER
}

export default function TrackFood({ food, day }: { food: TrackedFood[], day: string }) {
    const selectedFoodStore = useSelectedFoodStore();

    const foodWithAmount = food.filter((f) => f.amount > 0)
    const total = (nutrition: "kcal" | "protein" | "carbohydrates" | "fat") =>
        foodWithAmount.reduce((sum, f) => sum + getNutritionForAmount(f, nutrition), 0)
    const totalCalories = total("kcal")

    const closeDialog = () => {
        selectedFoodStore.unselect()
        selectedFoodStore.setShowSearch(false)
    }

    const openSearch = () => {
        selectedFoodStore.setMeal(currentMeal())
        selectedFoodStore.setShowSearch(true)
    }

    return (
        <div className="flex-1 w-full bg-muted/50 px-4 py-6 md:px-8">
            <div className="grid gap-8 lg:grid-cols-[minmax(0,22rem)_1fr]">
                <section>
                    <h1 className="text-4xl font-bold tracking-tight">{formatDayTitle(day)}</h1>
                    <p className="text-muted-foreground mt-1">{formatLongDay(day)}</p>

                    <div className="rounded-3xl bg-card shadow-sm p-6 mt-5">
                        <CalorieCard props={{ consumed: totalCalories, goal: CALORIE_GOAL, burned: 0 }} />
                        <div className="border-t my-5" />
                        <div className="flex flex-col gap-y-4">
                            <NutritionCard props={{ name: "Protein", color: "protein", consumed: total("protein"), goal: NUTRITION_GOALS.protein }} />
                            <NutritionCard props={{ name: "Kohlenhydrate", color: "carbohydrates", consumed: total("carbohydrates"), goal: NUTRITION_GOALS.carbohydrates }} />
                            <NutritionCard props={{ name: "Fett", color: "fat", consumed: total("fat"), goal: NUTRITION_GOALS.fat }} />
                        </div>
                    </div>
                </section>

                <section>
                    <div className="flex items-center justify-between lg:mt-8 mb-5">
                        <h2 className="text-2xl font-bold tracking-tight">Mahlzeiten</h2>
                        <button onClick={openSearch} className="flex items-center gap-x-2 rounded-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2.5">
                            <Plus className="size-5" />Essen hinzufügen
                        </button>
                    </div>
                    <div className="grid gap-5 md:grid-cols-2">
                        {MEALS.map((meal) =>
                            <MealView key={meal.type} props={{
                                meal: meal.type,
                                trackedFood: food.filter((f) => f.meal.type === meal.type),
                                remainingCalories: CALORIE_GOAL - totalCalories,
                                selectedFoodStore,
                            }} />
                        )}
                    </div>
                </section>
            </div>

            <FoodSearch meal={selectedFoodStore.meal ?? Meal.BREAKFAST} selectedFoodStore={selectedFoodStore} className="hidden" />

            <Dialog open={selectedFoodStore.foodId != null && selectedFoodStore.action === SelectedFoodAction.EDITING} onOpenChange={(open) => { if (!open) selectedFoodStore.unselect() }}>
                <DialogContent className="w-4xl flex flex-col">
                    <DialogTitle className="text-center text-lg">Eintrag bearbeiten</DialogTitle>
                    {selectedFoodStore.foodId != null && <>
                        <MealTabs meal={selectedFoodStore.meal ?? Meal.BREAKFAST} onChange={selectedFoodStore.setMeal} />
                        <div className="mt-2">
                            <FoodTrackView key={`${selectedFoodStore.foodId}-${selectedFoodStore.trackedFoodId}`} props={{
                                meal: selectedFoodStore.meal ?? Meal.BREAKFAST,
                                foodId: selectedFoodStore.foodId,
                                back: () => selectedFoodStore.setFoodId(null),
                                closeView: closeDialog,
                                selectedFoodStore,
                            }} />
                        </div>
                    </>}
                </DialogContent>
            </Dialog>
        </div>
    )
}
