'use client'
import NutritionCard, { NutritionCardProps } from "./NutritionCard"
import MealView from "./MealView"
import { getTrackedFood } from "./action"
import { useEffect, useState } from "react";
import { TrackedFood, Meal } from "@/utils/types/food";
import { getNutritionForAmount } from "@/utils/food";
import CalorieCard from "./CalorieCard";
import { useSelectedFoodStore } from "@/lib/zustand/selectedFood";
import FoodTrackView from "./FoodTrackView";
import { Dialog, DialogContent, DialogTrigger } from "@/components/ui/dialog";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import FoodSearch from "./FoodSearch";
import { Plus } from "lucide-react";


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

    const closeDialog = () => {
        selectedFoodStore.unselect()
        selectedFoodStore.setShowSearch(false)
    }

    const formattedDate = new Date().toLocaleDateString('de-DE');

    return (
        <div className="w-full h-[90vh] relative flex p-8 gap-x-4">
            <section className="w-1/3">
                <h1 className="text-2xl font-bold">Heute</h1>
                <p className="text-muted-foreground text-sm">{formattedDate}</p>
                <Card className={"w-full p-6 w-full mt-4"}>
                    <CalorieCard props={{ color: "red", consumed: totalCalories, goal: 3000, burned: 0 }} />
                    <Separator />
                    <div className="flex flex-col gap-y-3">
                        <NutritionCard props={{ name: "Protein", color: "protein", consumed: totalProtein, goal: 165 }} />
                        <NutritionCard props={{ name: "Kohlenhydrate", color: "carbohydrates", consumed: totalCarbs, goal: 360 }} />
                        <NutritionCard props={{ name: "Fett", color: "fat", consumed: totalFat, goal: 100 }} />
                    </div>
                </Card>
            </section>
            <section className="w-full mt-6">
                <h1 className="text-2xl font-bold mb-2">Mahlzeiten</h1>
                <div className="flex flex-wrap grid grid-cols-2 gap-4">
                    {foodLabels.map((labels) =>
                        <MealView key={labels.type} props={{ name: labels.type, trackedFood: trackedFood.get(labels.type) || [], selectedFoodStore }} />
                    )}
                </div>
            </section>
            <FoodSearch meal={selectedFoodStore.meal || Meal.BREAKFAST} selectedFoodStore={selectedFoodStore} className="bg-transparent flex justify-center items-center gap-x-1 mt-2">
            </FoodSearch>
        </div>
    )


}