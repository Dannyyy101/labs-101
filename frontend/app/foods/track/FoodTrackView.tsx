import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { Food } from "@/utils/types/food"
import { Input } from "@base-ui/react"
import { ArrowLeft } from "lucide-react"
import { useState } from "react"
import { createTrackFood } from "./action"

interface FoodTrackViewProps {
    meal: string,
    food: Food
    closeView: () => void
    back: () => void
}

export default function FoodTrackView({ props }: { props: FoodTrackViewProps }) {
    const [amount, setAmount] = useState<number>(100)


    const trackFood = async () => {
        await createTrackFood({ ...props.food, amount, meal: props.meal })
        props.closeView()
    }


    return <div>
        <Button onClick={props.back} variant={"secondary"}><ArrowLeft /> Search</Button>
        <div>
            <h1 className="text-xl font-semibold">{props.food.name}</h1>
            <h2 className="text-center text-4xl font-semibold">{props.food.kcal}</h2>
            <p className="text-center text-muted-foreground">kcal</p>
            <div className="flex justify-center gap-x-8 mt-4">
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">PROTEIN</p>
                    <h3 className="text-protein font-bold text-lg">{Math.round(props.food.protein / amount * 100)}g</h3>
                </div>
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">CH</p>
                    <h3 className="text-carbohydrates font-bold text-lg">{Math.round(props.food.carbohydrates / amount * 100)}g</h3>
                </div>
                <div>
                    <p className="text-center text-sm text-muted-foreground font-semibold">FAT</p>
                    <h3 className="text-fat font-bold text-lg">{Math.round(props.food.fat / amount * 100)}g</h3>
                </div>
            </div>
            <div className="mt-4">
                <Field>
                    <FieldLabel className="text-sm text-muted-foreground font-semibold" htmlFor="input-field-username">AMOUNT</FieldLabel>
                    <Input
                        id="input-field-username"
                        className={"focus:outline-none border rounded-md pl-2 border-2"}
                        type="number"
                        value={amount}
                        onChange={(e) => setAmount(parseInt(e.target.value))}
                    />
                </Field>
            </div>
            <Button className={"w-full mt-2"} onClick={trackFood}>Add</Button>
        </div>
    </div>
}