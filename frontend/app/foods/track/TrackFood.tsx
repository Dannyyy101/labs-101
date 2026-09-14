'use client'
import NutritionCard, { NutritionCardProps } from "./NutritionCard"
import Meal from "./Meal"
import { getTrackedFood } from "./action"
import { useEffect, useState } from "react";
import { FoodWithAmount } from "@/utils/types/food";


export default function TrackFood({ food }: { food: FoodWithAmount[] }) {
    const [trackedFood, setTrackedFood] = useState<Map<string, FoodWithAmount[]>>(new Map())

    useEffect(() => {

        const map = new Map<string, FoodWithAmount[]>()
        for (const f of food) {
            const list = map.get(f.meal.type)
            if (list) list.push(f)
            else map.set(f.meal.type, [f])
        }
        setTrackedFood(map)

    }, [food])

    const foodLabels = [{ type: "BREAKFAST" }, { type: "LUNCH" }, { type: "DINNER" }, { type: "SNACK" }]

    const foodWithAmount = food.filter((f) => f.amount > 0)
    const totalProtein = Math.round(foodWithAmount.reduce((partialSum, a) => partialSum + (a.protein || 0) * a.amount / 100, 0));
    const totalCarbs = Math.round(foodWithAmount.reduce((partialSum, a) => partialSum + (a.carbohydrates || 0) * a.amount / 100, 0));
    const totalFat = Math.round(foodWithAmount.reduce((partialSum, a) => partialSum + (a.fat || 0) * a.amount / 100, 0));

    const nutritionCardsProps: NutritionCardProps[] = [
        { name: "Protein", value: totalProtein, goal: 100, color: "red" },
        { name: "Kohlenhydrate", value: totalCarbs, goal: 100, color: "orange" },
        { name: "Fett", value: totalFat, goal: 100, color: "blue" }
    ]

    return (
        <div className="w-full h-[90vh] relative">
            <div className="w-full flex justify-center gap-x-4">
                {nutritionCardsProps.map((prop) => <NutritionCard key={prop.name} props={prop} />)}
            </div>
            <section className="mt-8 w-full">
                {foodLabels.map((labels) =>
                    <Meal key={labels.type} props={{ name: labels.type, trackedFood: trackedFood.get(labels.type) || [] }} />
                )}
            </section>
        </div>
    )


}