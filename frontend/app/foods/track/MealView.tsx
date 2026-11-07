import { Meal, TrackedFood } from "@/utils/types/food"
import { Minus, Plus } from "lucide-react"
import { getNutritionForAmount } from "@/utils/food"
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood"
import { useTransition } from "react"
import { deleteTrackedFood } from "./action"
import { formatNumber, formatTime } from "./format"
import { Spinner } from "@/components/ui/spinner"

export const MEALS = [
    // the chart series in a fixed order, red stays reserved for problems
    { type: Meal.BREAKFAST, label: "Breakfast", dot: "bg-series-2" },
    { type: Meal.LUNCH, label: "Lunch", dot: "bg-series-1" },
    { type: Meal.SNACK, label: "Snack", dot: "bg-series-3" },
    { type: Meal.DINNER, label: "Dinner", dot: "bg-series-4" },
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

    return <section className="min-w-0 glass rounded-[14px] flex flex-col">
        <header className="flex items-center gap-x-2 mx-6 py-4 border-b">
            <span className={`size-2.5 rounded-full ${meal.dot}`} />
            <h3 className="text-heading">{meal.label}</h3>
            {firstEntry && <span className="text-body text-ink-muted tabular-nums">{formatTime(firstEntry)}</span>}
            <p className="ml-auto"><span className="text-heading tabular-nums">{formatNumber(kcal)}</span> <span className="text-body text-ink-muted">kcal</span></p>
        </header>

        <div className="flex-1 min-h-24">
            {props.trackedFood.length === 0
                ? <p className="mx-6 py-4 text-ink-muted">Noch nichts eingetragen. {formatNumber(Math.max(props.remainingCalories, 0))} kcal stehen dir noch zur Verfügung.</p>
                : <ul className="divide-y mx-6">
                    {props.trackedFood.map((food) => <FoodRow key={food.id} trackedFood={food} selectedFoodStore={props.selectedFoodStore} />)}
                </ul>
            }
        </div>

        <footer className="border-t px-6 py-3">
            <button className="flex items-center gap-x-2 text-brand-strong hover:text-brand-strong" onClick={showSearch}>
                <Plus className="size-5 rounded-full bg-brand text-on-brand p-0.5" />
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
            <p className="text-body text-ink-muted tabular-nums">{amount} · P {nutrition("protein")} · K {nutrition("carbohydrates")} · F {nutrition("fat")}</p>
        </button>
        <span className="text-ink-muted tabular-nums">{formatNumber(getNutritionForAmount(trackedFood, "kcal"))}</span>
        <button
            aria-label={`${trackedFood.food.name} entfernen`}
            className="size-6 shrink-0 rounded-full glass-inset text-ink-muted flex items-center justify-center hover:bg-destructive/15 hover:text-destructive"
            disabled={deleting}
            onClick={() => startDelete(() => deleteTrackedFood(trackedFood.id))}
        >
            {deleting ? <Spinner className="size-3" /> : <Minus className="size-3.5" />}
        </button>
    </li>
}
