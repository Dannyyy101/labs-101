import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { CreateFoodWithAmount, Food, FoodPortion, FoodWithAmount, FoodWithPortion } from "@/utils/types/food"
import { Input } from "@base-ui/react"
import { ArrowLeft } from "lucide-react"
import { useEffect, useState } from "react"
import { createTrackFood, getTrackedFoodByFoodId } from "./action"
import { Badge } from "@/components/ui/badge"
import { getNutritionForAmount } from "@/utils/food"

interface FoodTrackViewProps {
    meal: string,
    foodId: number
    closeView: () => void
    back: () => void
}

export default function FoodTrackView({ props }: { props: FoodTrackViewProps }) {
    const [food, setFood] = useState<FoodWithPortion>()
    const [trackFood, setTrackFood] = useState<CreateFoodWithAmount | null>(null)
    const [selectedPortion, setSelectedPortion] = useState<FoodPortion | null>(null)

    useEffect(() => {
        const fetch = async () => {
            const food = await getTrackedFoodByFoodId(props.foodId)
            const { lastEntry, ...rest } = food
            setFood({ ...rest })
            setTrackFood({ foodId: food.id, userId: "", amount: lastEntry?.amount || 100, portionId: null, meal: props.meal })
        }
        fetch()
    }, [])

    if (!food || !trackFood)
        return <></>

    const saveTrackedFood = async () => {
        await createTrackFood(trackFood)
        props.closeView()
    }

    const selectPortion = (portion: FoodPortion | null) => {
        if (portion) {
            setTrackFood(() => ({ ...trackFood, portionId: trackFood.portionId === portion?.id ? null : portion.id }))
            setSelectedPortion(portion)
        }
    }

    return <div>
        <Button onClick={props.back} variant={"secondary"}><ArrowLeft /> Search</Button>
        <div>
            <h1 className="text-xl font-semibold">{food.name}</h1>
            <h2 className="text-center text-4xl font-semibold">{getNutritionForAmount({ ...food, amount: trackFood.amount, meal: { type: "", typeLabel: "" }, portion: selectedPortion }, "kcal")}</h2>
            <p className="text-center text-muted-foreground">kcal</p>
            <div className="flex justify-center gap-x-8 mt-4">
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">PROTEIN</p>
                    <h3 className="text-protein font-bold text-lg">{getNutritionForAmount({ ...food, amount: trackFood.amount, meal: { type: "", typeLabel: "" }, portion: selectedPortion }, "protein")}g</h3>
                </div>
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">CH</p>
                    <h3 className="text-carbohydrates font-bold text-lg">{getNutritionForAmount({ ...food, amount: trackFood.amount, meal: { type: "", typeLabel: "" }, portion: selectedPortion }, "carbohydrates")}g</h3>
                </div>
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">FAT</p>
                    <h3 className="text-fat font-bold text-lg">{getNutritionForAmount({ ...food, amount: trackFood.amount, meal: { type: "", typeLabel: "" }, portion: selectedPortion }, "fat")}g</h3>
                </div>
            </div>
            <div className="mt-4">
                <Field>
                    <FieldLabel className="text-sm text-muted-foreground font-semibold" htmlFor="input-field-username">AMOUNT</FieldLabel>
                    <div className="flex gap-x-2">
                        <Badge className="hover:cursor-pointer" variant={trackFood.portionId === null ? "default" : "outline"} onClick={() => selectPortion(null)}>Gram</Badge>
                        {food.portions.map((portion) =>
                            <Badge className="hover:cursor-pointer" variant={trackFood.portionId === portion.id ? "default" : "outline"} onClick={() => selectPortion(portion)} key={portion.id}>{portion.label}</Badge>
                        )}

                    </div>
                    <Input
                        id="input-field-username"
                        className={"focus:outline-none border rounded-md pl-2 border-2"}
                        type="number"
                        value={trackFood.amount}
                        onChange={(e) => setTrackFood({ ...trackFood, amount: parseInt(e.target.value) || 0 })}
                    />
                </Field>
            </div>
            <Button className={"w-full mt-2"} onClick={saveTrackedFood}>Add</Button>
        </div>
    </div>
}