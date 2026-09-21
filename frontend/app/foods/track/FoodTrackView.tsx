import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { CreateFoodWithAmount, CreateTrackedFood, FoodPortion, FoodWithPortion, Meal, TrackedFood } from "@/utils/types/food"
import { Input } from "@base-ui/react"
import { ArrowLeft } from "lucide-react"
import { useEffect, useState } from "react"
import { createTrackFood, deleteTrackedFood, getTrackedFoodByFoodId, getTrackedFoodByTrackedFoodId, updateTrackFood } from "./action"
import { Badge } from "@/components/ui/badge"
import { getNutritionForAmount } from "@/utils/food"
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood"
import { Spinner } from "@/components/ui/spinner"

interface FoodTrackViewProps {
    meal: Meal,
    foodId: number
    closeView: () => void
    back: () => void
    selectedFoodStore: SelectedFoodState
}

export default function FoodTrackView({ props }: { props: FoodTrackViewProps }) {
    const [food, setFood] = useState<FoodWithPortion>()
    const [trackFood, setTrackFood] = useState<CreateTrackedFood | null>(null)
    const [selectedPortion, setSelectedPortion] = useState<FoodPortion | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const fetch = async () => {
            const food = await getTrackedFoodByFoodId(props.foodId)
            const { lastEntry, ...rest } = food
            setFood({ ...rest })
            if (props.selectedFoodStore.trackedFoodId) {
                const trackedFood = await getTrackedFoodByTrackedFoodId(props.selectedFoodStore.trackedFoodId);
                setSelectedPortion(food.portions.find((portion) => portion.id === trackedFood?.portion?.id) || null)
                setTrackFood({ id: trackedFood.id, foodId: trackedFood.food.id, amount: trackedFood.amount, portionId: trackedFood.portion?.id || null, meal: props.meal })
            } else {
                setTrackFood({ id: props.selectedFoodStore.trackedFoodId, foodId: props.foodId, amount: lastEntry?.amount || 100, portionId: null, meal: props.meal })
            }
            setLoading(false)

        }
        fetch()
    }, [])

    if (!food || !trackFood)
        return <></>

    const saveTrackedFood = async () => {
        await createTrackFood(trackFood)
        props.closeView()
    }

    const updateTrackedFood = async () => {
        await updateTrackFood(trackFood)
        props.closeView()
    }


    const handleDeleteSelectedFood = async () => {
        if (props.selectedFoodStore.trackedFoodId)
            await deleteTrackedFood(props.selectedFoodStore.trackedFoodId)
        props.closeView()
    }

    const selectPortion = (portion: FoodPortion | null) => {
        if (portion) {
            setTrackFood(() => ({ ...trackFood, portionId: trackFood.portionId === portion?.id ? null : portion.id }))
            setSelectedPortion(trackFood.portionId === portion?.id ? null : portion)
        }
        else {
            setTrackFood(() => ({ ...trackFood, portionId: null }))
            setSelectedPortion(null)
        }

    }

    if (loading)
        return <Spinner></Spinner>
    return <div>
        {props.selectedFoodStore.action === SelectedFoodAction.CREATING &&
            <Button onClick={props.back} variant={"secondary"}><ArrowLeft /> Search</Button>
        }
        <div>
            <h1 className="text-xl font-semibold">{food.name}</h1>
            <h2 className="text-center text-4xl font-semibold">{getNutritionForAmount(({ ...trackFood, portion: selectedPortion, food }), "kcal")}</h2>
            <p className="text-center text-muted-foreground">kcal</p>
            <div className="flex justify-center gap-x-8 mt-4">
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">PROTEIN</p>
                    <h3 className="text-protein font-bold text-lg">{getNutritionForAmount(({ ...trackFood, portion: selectedPortion, food }), "protein")}g</h3>
                </div>
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">CH</p>
                    <h3 className="text-carbohydrates font-bold text-lg">{getNutritionForAmount(({ ...trackFood, portion: selectedPortion, food }), "carbohydrates")}g</h3>
                </div>
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">FAT</p>
                    <h3 className="text-fat font-bold text-lg">{getNutritionForAmount(({ ...trackFood, portion: selectedPortion, food }), "fat")}g</h3>
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
                        className="focus:outline-none border rounded-md pl-2 border-2"
                        type="number"
                        value={trackFood.amount === 0 ? "" : trackFood.amount}
                        onChange={(e) => {
                            const val = e.target.value;
                            setTrackFood({
                                ...trackFood,
                                amount: val === "" ? 0 : parseInt(val)
                            });
                        }}
                    />
                </Field>
            </div>
            <Button className={"w-full mt-2"} onClick={props.selectedFoodStore.action === SelectedFoodAction.CREATING ? saveTrackedFood : updateTrackedFood}>{props.selectedFoodStore.action === SelectedFoodAction.CREATING ? "Add" : "Update"}</Button>
            {props.selectedFoodStore.action === SelectedFoodAction.EDITING &&
                <Button variant={"destructive"} className={"w-full mt-2"} onClick={handleDeleteSelectedFood}>Delete</Button>
            }

        </div>
    </div >
}