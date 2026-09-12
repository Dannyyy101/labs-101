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
            const list = map.get(f.meal)
            if (list) list.push(f)
            else map.set(f.meal, [f])
        }
        setTrackedFood(map)

    }, [food])


    const foodWithAmount = food.filter((f) => f.amount > 0)
    const totalProtein = Math.round(foodWithAmount.reduce((partialSum, a) => partialSum + a.protein * a.amount / 100, 0));
    const totalCarbs = Math.round(foodWithAmount.reduce((partialSum, a) => partialSum + a.carbohydrates * a.amount / 100, 0));
    const totalFat = Math.round(foodWithAmount.reduce((partialSum, a) => partialSum + a.fat * a.amount / 100, 0));

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
                {["Frühstück", "Mittagessen", "Abendessen", "Snack"].map((key) =>
                    <Meal key={key} props={{ name: key, trackedFood: trackedFood.get(key) || [] }} />
                )}
            </section>
        </div>
    )


}