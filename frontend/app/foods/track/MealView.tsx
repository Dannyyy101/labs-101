import { Meal, TrackedFood } from "@/utils/types/food"
import { Minus, Plus } from "lucide-react"
import { getNutritionForAmount } from "@/utils/food"
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood"
import { useTransition } from "react"
import { deleteTrackedFood } from "./action"
import { formatNumber, formatTime } from "./format"
import { Spinner } from "@/components/ui/spinner"

export const MEALS = [
    { type: Meal.BREAKFAST, label: "Breakfast", dot: "bg-orange-400" },
    { type: Meal.LUNCH, label: "Lunch", dot: "bg-blue-500" },
    { type: Meal.SNACK, label: "Snack", dot: "bg-green-500" },
    { type: Meal.DINNER, label: "Dinner", dot: "bg-red-500" },
]

export interface MealProps {
    meal: Meal
    trackedFood: TrackedFood[]
    remainingCalories: number
    selectedFoodStore: SelectedFoodState
}

export default function MealView({ props }: { props: MealProps }) {
    const meal = MEALS.find((m) => m.type === props.meal)!
    const kcal = props.trackedFood.reduce((sum, f) => sum + getNutritionForAmount(f, "kcal"), 0)
    const firstEntry = props.trackedFood.map((f) => f.createDate).filter(Boolean).sort()[0]

    const showSearch = () => {
        props.selectedFoodStore.setMeal(props.meal)
        props.selectedFoodStore.setShowSearch(true)
    }

    return <section className="rounded-3xl bg-card shadow-sm flex flex-col">
        <header className="flex items-center gap-x-2 mx-6 py-4 border-b">
            <span className={`size-2.5 rounded-full ${meal.dot}`} />
            <h3 className="text-lg font-semibold">{meal.label}</h3>
            {firstEntry && <span className="text-sm text-muted-foreground tabular-nums">{formatTime(firstEntry)}</span>}
            <p className="ml-auto"><span className="text-lg font-semibold tabular-nums">{formatNumber(kcal)}</span> <span className="text-sm text-muted-foreground">kcal</span></p>
        </header>

        <div className="flex-1 min-h-24">
            {props.trackedFood.length === 0
                ? <p className="mx-6 py-4 text-muted-foreground">Noch nichts eingetragen. {formatNumber(Math.max(props.remainingCalories, 0))} kcal stehen dir noch zur Verfügung.</p>
                : <ul className="divide-y mx-6">
                    {props.trackedFood.map((food) => <FoodRow key={food.id} trackedFood={food} selectedFoodStore={props.selectedFoodStore} />)}
                </ul>
            }
        </div>

        <footer className="border-t px-6 py-3">
            <button className="flex items-center gap-x-2 text-blue-500 hover:text-blue-600" onClick={showSearch}>
                <Plus className="size-5 rounded-full bg-blue-500 text-white p-0.5" />
                <span>Hinzufügen</span>
            </button>
        </footer>
    </section>
}

function FoodRow({ trackedFood, selectedFoodStore }: { trackedFood: TrackedFood, selectedFoodStore: SelectedFoodState }) {
    const [deleting, startDelete] = useTransition()
    const nutrition = (key: "protein" | "carbohydrates" | "fat") => formatNumber(getNutritionForAmount(trackedFood, key))

    const amount = trackedFood.portion
        ? `${formatNumber(trackedFood.amount)} ${trackedFood.portion.label} · ${formatNumber(trackedFood.amount * trackedFood.portion.grams)} g`
        : `${formatNumber(trackedFood.amount)} g`

    const edit = () => selectedFoodStore.selectFood(trackedFood.meal.type as Meal, trackedFood.food.id, SelectedFoodAction.EDITING, trackedFood.id)

    return <li className="flex items-center gap-x-3 py-3" style={{ opacity: deleting ? 0.5 : 1 }}>
        <button onClick={edit} className="flex-1 min-w-0 text-left">
            <p className="truncate">{trackedFood.food.name}</p>
            <p className="text-sm text-muted-foreground tabular-nums">{amount} · P {nutrition("protein")} · K {nutrition("carbohydrates")} · F {nutrition("fat")}</p>
        </button>
        <span className="text-muted-foreground tabular-nums">{formatNumber(getNutritionForAmount(trackedFood, "kcal"))}</span>
        <button
            aria-label={`${trackedFood.food.name} entfernen`}
            className="size-6 shrink-0 rounded-full bg-muted text-muted-foreground flex items-center justify-center hover:bg-destructive/15 hover:text-destructive"
            disabled={deleting}
            onClick={() => startDelete(() => deleteTrackedFood(trackedFood.id))}
        >
            {deleting ? <Spinner className="size-3" /> : <Minus className="size-3.5" />}
        </button>
    </li>
}
