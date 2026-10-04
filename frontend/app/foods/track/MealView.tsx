import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible"
import { Meal, Meal as MealType, TrackedFood } from "@/utils/types/food"
import { ChevronDownIcon, Plus } from "lucide-react"
import FoodSearch from "./FoodSearch"
import { getNutritionForAmount } from "@/utils/food"
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood"
import { Separator } from "@/components/ui/separator"

export interface MealProps {
    name: string
    trackedFood: TrackedFood[]
    selectedFoodStore: SelectedFoodState
}

export default function MealView({ props }: { props: MealProps }) {

    const showSearch = () => {
        props.selectedFoodStore.setMeal(props.name as unknown as Meal)
        props.selectedFoodStore.setShowSearch(true)
    }

    return <Card className="w-full relative">
        <CardHeader className="flex justify-between">
            <h2 className="font-semibold">{props.name}</h2>
            <div className="flex"><p className="font-semibold">100</p> <p className="text-muted-foreground text-sm">kcal</p></div>
        </CardHeader>

        <CardContent className="min-h-20">
            <div className="w-full flex flex-col gap-2">
                <Separator />
                {props.trackedFood.length === 0 ? <p className="text-muted-foreground text-sm">Noch nichts eingetragen.</p> :
                    <>
                        {props.trackedFood.map((food, index) => <Food key={`${food.id}-${index}`} trackedFood={food} selectedFoodStore={props.selectedFoodStore} />)}
                    </>
                }
            </div>
        </CardContent>
        <div className="absolute bottom-0 w-full p-4">
            <Separator />
            <button className="flex items-center gap-x-1 mt-2" onClick={showSearch}>
                <Plus className="size-4 rounded-full text-white bg-blue-400 p-0.5" />
                <p className="text-blue-400">Hinzufügen</p>
            </button>
        </div>

    </Card >
}

function Food({ trackedFood, selectedFoodStore }: { trackedFood: TrackedFood, selectedFoodStore: SelectedFoodState }) {
    return <button onClick={() => selectedFoodStore.selectFood(trackedFood.meal.type as any, trackedFood.food.id, SelectedFoodAction.EDITING, trackedFood.id)} className="w-full flex text-left">
        <div className="w-full">
            <p className="">{trackedFood.food.name}</p>
            <div className="flex">
                <p className="pr-1 text-xs text-muted-foreground">{trackedFood.portion ? `${trackedFood.amount} ${trackedFood.portion.label}` : `${trackedFood.amount}g`}</p>
                <p className="text-muted-foreground">·</p>
                <p className="px-1 text-xs text-muted-foreground">P {getNutritionForAmount(trackedFood, "protein")}</p>
                <p className="text-muted-foreground">·</p>
                <p className="px-1 text-xs text-muted-foreground">KH {getNutritionForAmount(trackedFood, "carbohydrates")}</p>
                <p className="text-muted-foreground">·</p>
                <p className="pl-1 text-xs text-muted-foreground">F {getNutritionForAmount(trackedFood, "fat")}</p>
            </div>
        </div>
        <div className="w-10">
            <p className="text-md text-right">{getNutritionForAmount(trackedFood, "kcal")}</p>
        </div>
    </button>
}