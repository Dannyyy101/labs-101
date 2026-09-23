'use client'
import NutritionCard, { NutritionCardProps } from "./NutritionCard"
import MealView from "./Meal"
import { getTrackedFood } from "./action"
import { useEffect, useState } from "react";
import { TrackedFood, Meal } from "@/utils/types/food";
import { getNutritionForAmount } from "@/utils/food";
import CalorieCard from "./CalorieCard";
import { useSelectedFoodStore } from "@/lib/zustand/selectedFood";
import FoodTrackView from "./FoodTrackView";
import { Dialog, DialogContent, DialogTrigger } from "@/components/ui/dialog";


export default function TrackFood({ food }: { food: TrackedFood[] }) {
    const [trackedFood, setTrackedFood] = useState<Map<string, TrackedFood[]>>(new Map())
    const selectedFoodStore = useSelectedFoodStore();

    useEffect(() => {

        const map = new Map<string, TrackedFood[]>()
        for (const f of food) {
            const list = map.get(f.meal.type)
            if (list) list.push(f)
            else map.set(f.meal.type, [f])
        }
        setTrackedFood(map)

    }, [food])

    const foodLabels = [
        { type: "BREAKFAST", label: "Breakfast" },
        { type: "LUNCH", label: "Lunch" },
        { type: "DINNER", label: "Dinner" },
        { type: "SNACK", label: "Snack" }]

    const foodWithAmount = food.filter((f) => f.amount > 0)
    const totalProtein = foodWithAmount.reduce((partialSum, a) => partialSum + getNutritionForAmount(a, "protein"), 0);
    const totalCarbs = foodWithAmount.reduce((partialSum, a) => partialSum + getNutritionForAmount(a, "carbohydrates"), 0);
    const totalFat = foodWithAmount.reduce((partialSum, a) => partialSum + getNutritionForAmount(a, "fat"), 0);
    const totalCalories = foodWithAmount.reduce((partialSum, a) => partialSum + getNutritionForAmount(a, "kcal"), 0);


    const nutritionCardsProps: NutritionCardProps[] = [
        { name: "Protein", value: totalProtein, goal: 165, color: "red" },
        { name: "Kohlenhydrate", value: totalCarbs, goal: 360, color: "orange" },
        { name: "Fett", value: totalFat, goal: 100, color: "blue" }
    ]

    const closeDialog = () => {
        selectedFoodStore.unselect()
        selectedFoodStore.setShowSearch(false)
    }

    return (
        <div className="w-full h-[90vh] relative flex flex-col items-center">
            <div className="p-4 w-full md:w-4/6">
                <Dialog open={selectedFoodStore.foodId != null} onOpenChange={selectedFoodStore.unselect}>
                    <DialogContent className="w-4xl flex flex-col ">
                        <section>
                            <FoodTrackView props={{ meal: selectedFoodStore.meal || Meal.BREAKFAST, foodId: selectedFoodStore.foodId || -1, back: () => selectedFoodStore.unselect(), closeView: closeDialog, selectedFoodStore }} />
                        </section>
                    </DialogContent>
                </Dialog>

                <div className="w-full my-2">
                    <CalorieCard props={{ name: "Calories", color: "red", consumed: totalCalories, goal: 3000, burned: 0 }} />
                </div>
                <div className="w-full flex justify-center gap-x-4">
                    {nutritionCardsProps.map((prop) => <NutritionCard key={prop.name} props={prop} />)}
                </div>
                <section className="mt-4 w-full">
                    {foodLabels.map((labels) =>
                        <MealView key={labels.type} props={{ name: labels.type, trackedFood: trackedFood.get(labels.type) || [], selectedFoodStore }} />
                    )}
                </section>
            </div>
        </div>
    )


}