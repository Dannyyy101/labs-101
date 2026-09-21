import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible"
import { Meal as MealType, TrackedFood } from "@/utils/types/food"
import { ChevronDownIcon } from "lucide-react"
import FoodSearch from "./FoodSearch"
import { getNutritionForAmount } from "@/utils/food"
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood"

export interface MealProps {
    name: string
    trackedFood: TrackedFood[]
    selectedFoodStore: SelectedFoodState
}

export default function Meal({ props }: { props: MealProps }) {
    return <Card className="w-full mt-2">
        <CardContent>
            <Collapsible className="rounded-md">
                <CollapsibleTrigger render={<Button variant="ghost" className="w-full">{props.name}
                    <ChevronDownIcon className="ml-auto group-data-panel-open/button:rotate-180" /></Button>} />
                <CollapsibleContent className="flex flex-col items-start gap-2 p-2.5 pt-0 text-sm">
                    <div className="w-full">
                        {props.trackedFood.map((food, index) => <Food key={`${food.id}-${index}`} trackedFood={food} selectedFoodStore={props.selectedFoodStore} />)}
                    </div>
                    <FoodSearch selectedFoodStore={props.selectedFoodStore} meal={props.name}></FoodSearch>
                </CollapsibleContent>
            </Collapsible>
        </CardContent>
    </Card>
}

function Food({ trackedFood, selectedFoodStore }: { trackedFood: TrackedFood, selectedFoodStore: SelectedFoodState }) {
    return <button onClick={() => selectedFoodStore.selectFood(trackedFood.meal.type as any, trackedFood.food.id, SelectedFoodAction.EDITING, trackedFood.id)} className="w-full flex text-left">
        <div className="w-full">
            <h3 className="text-lg font-semibold">{trackedFood.food.name}</h3>
            <div className="flex">
                <p className="border-r-2 pr-2">{trackedFood.portion ? `${trackedFood.amount} ${trackedFood.portion.label}` : `${trackedFood.amount}g`}</p>
                <p className="border-r-2 px-2">P {getNutritionForAmount(trackedFood, "protein")}</p>
                <p className="border-r-2 px-2">KH {getNutritionForAmount(trackedFood, "carbohydrates")}</p>
                <p className="pl-2">F {getNutritionForAmount(trackedFood, "fat")}</p>
            </div>
        </div>
        <div className="w-10">
            <p className="text-lg font-semibold text-right">{getNutritionForAmount(trackedFood, "kcal")}</p>
            <p className="text-muted-foreground text-right -mt-1">kcal</p>
        </div>
    </button>
}