import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible"
import { FoodWithAmount } from "@/utils/types/food"
import { ChevronDownIcon } from "lucide-react"
import FoodSearch from "./FoodSearch"
import { getNutritionForAmount } from "@/utils/food"

export interface MealProps {
    name: string
    trackedFood: FoodWithAmount[]
}

export default function Meal({ props }: { props: MealProps }) {
    return <Card className="w-full mt-2">
        <CardContent>
            <Collapsible className="rounded-md">
                <CollapsibleTrigger render={<Button variant="ghost" className="w-full">{props.name}
                    <ChevronDownIcon className="ml-auto group-data-panel-open/button:rotate-180" /></Button>} />
                <CollapsibleContent className="flex flex-col items-start gap-2 p-2.5 pt-0 text-sm">
                    <div className="w-full">
                        {props.trackedFood.map((food, index) => <Food key={`${food.id}-${index}`} foodWithAmount={food} />)}
                    </div>
                    <FoodSearch meal={props.name}></FoodSearch>
                </CollapsibleContent>
            </Collapsible>
        </CardContent>
    </Card>
}

function Food({ foodWithAmount }: { foodWithAmount: FoodWithAmount }) {
    return <div className="w-full flex">
        <div className="w-full">
            <h3 className="text-lg font-semibold">{foodWithAmount.name}</h3>
            <div className="flex">
                <p className="border-r-2 pr-2">{foodWithAmount.portion ? `${foodWithAmount.amount} ${foodWithAmount.portion.label}` : `${foodWithAmount.amount}g`}</p>
                <p className="border-r-2 px-2">P {getNutritionForAmount(foodWithAmount, "protein")}</p>
                <p className="border-r-2 px-2">KH {getNutritionForAmount(foodWithAmount, "carbohydrates")}</p>
                <p className="pl-2">F {getNutritionForAmount(foodWithAmount, "fat")}</p>
            </div>
        </div>
        <div className="w-10">
            <p className="text-lg font-semibold text-right">{getNutritionForAmount(foodWithAmount, "kcal")}</p>
            <p className="text-muted-foreground text-right -mt-1">kcal</p>
        </div>
    </div>
}