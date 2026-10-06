import { CreateTrackedFood, FoodPortion, FoodWithPortion, Meal } from "@/utils/types/food"
import { ChevronLeft, Minus, Plus, Trash2 } from "lucide-react"
import { useEffect, useState } from "react"
import { addFoodPortion, createTrackFood, deleteTrackedFood, getOpenFood, getTrackedFoodByFoodId, getTrackedFoodByTrackedFoodId, importOpenFood, trackOpenFood, updateTrackFood } from "./action"
import { getNutritionForAmount } from "@/utils/food"
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood"
import { Spinner } from "@/components/ui/spinner"
import { MEALS } from "./MealView"
import { formatNumber } from "./format"

interface FoodTrackViewProps {
    meal: Meal,
    foodId: number | null
    // an open food is only imported into our foods when it gets tracked
    openFoodId?: number | null
    closeView: () => void
    back: () => void
    selectedFoodStore: SelectedFoodState
}

const NUTRIENTS = [
    { key: "kcal", label: "Kalorien", unit: "" },
    { key: "protein", label: "Protein", unit: " g" },
    { key: "carbohydrates", label: "Carbs", unit: " g" },
    { key: "fat", label: "Fett", unit: " g" },
] as const

export default function FoodTrackView({ props }: { props: FoodTrackViewProps }) {
    const [food, setFood] = useState<FoodWithPortion>()
    const [trackFood, setTrackFood] = useState<CreateTrackedFood | null>(null)
    const [selectedPortion, setSelectedPortion] = useState<FoodPortion | null>(null)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [showPortionForm, setShowPortionForm] = useState(false)
    const [openFoodId, setOpenFoodId] = useState(props.openFoodId ?? null)

    const creating = props.selectedFoodStore.action === SelectedFoodAction.CREATING

    useEffect(() => {
        const fetch = async () => {
            if (openFoodId !== null) {
                setFood(await getOpenFood(openFoodId))
                setTrackFood({ foodId: 0, amount: 100, portionId: null, meal: props.meal })
                setLoading(false)
                return
            }
            const food = await getTrackedFoodByFoodId(props.foodId!)
            const { lastEntry, ...rest } = food
            setFood({ ...rest })
            if (props.selectedFoodStore.trackedFoodId) {
                const trackedFood = await getTrackedFoodByTrackedFoodId(props.selectedFoodStore.trackedFoodId);
                setSelectedPortion(food.portions.find((portion) => portion.id === trackedFood?.portion?.id) || null)
                setTrackFood({ id: trackedFood.id, foodId: trackedFood.food.id, amount: trackedFood.amount, portionId: trackedFood.portion?.id || null, meal: props.meal })
            } else {
                setTrackFood({ id: props.selectedFoodStore.trackedFoodId, foodId: props.foodId!, amount: lastEntry?.amount || 100, portionId: null, meal: props.meal })
            }
            setLoading(false)

        }
        fetch()
    }, [])

    if (loading || !food || !trackFood)
        return <div className="flex justify-center py-10"><Spinner /></div>

    // the meal can still be switched with the tabs after the food got selected
    const entry = { ...trackFood, meal: props.meal }
    const withAmount = { ...trackFood, portion: selectedPortion, food }
    const grams = trackFood.amount * (selectedPortion?.grams ?? 1)
    const unit = selectedPortion?.label ?? "g"
    const step = selectedPortion ? 1 : 10

    const save = async () => {
        setSaving(true)
        if (openFoodId !== null) await trackOpenFood(openFoodId, { amount: entry.amount, portionId: entry.portionId, meal: entry.meal })
        else if (creating) await createTrackFood(entry)
        else await updateTrackFood(entry)
        props.closeView()
    }

    const handleDeleteSelectedFood = async () => {
        setSaving(true)
        if (props.selectedFoodStore.trackedFoodId)
            await deleteTrackedFood(props.selectedFoodStore.trackedFoodId)
        props.closeView()
    }

    // keeps the grams when switching to gram, starts with one piece when switching to a portion
    const selectPortion = (portion: FoodPortion | null) => {
        setTrackFood({ ...trackFood, portionId: portion?.id ?? null, amount: portion ? 1 : Math.round(grams) })
        setSelectedPortion(portion)
    }

    const setAmount = (amount: number) => setTrackFood({ ...trackFood, amount: Math.max(0, Math.round(amount * 10) / 10) })

    // a portion needs a food, so an open food gets imported first
    const importFood = async () => {
        if (openFoodId === null) return food.id
        const imported = await importOpenFood(openFoodId)
        setOpenFoodId(null)
        setFood(imported)
        setTrackFood({ ...trackFood, foodId: imported.id })
        return imported.id
    }

    const portionAdded = (portions: FoodPortion[], label: string) => {
        setFood((prev) => prev && { ...prev, portions })
        setShowPortionForm(false)
        const portion = portions.find((p) => p.label.trim().toLowerCase() === label.trim().toLowerCase())
        if (portion) selectPortion(portion)
    }

    const per100g = (key: "protein" | "carbohydrates" | "fat") => formatNumber(food[key] ?? 0)
    const mealLabel = MEALS.find((m) => m.type === props.meal)?.label

    return <div className="flex flex-col">
        {creating &&
            <button onClick={props.back} className="flex items-center gap-x-1 text-blue-500 hover:text-blue-600 self-start">
                <ChevronLeft className="size-5" />Alle Lebensmittel
            </button>
        }

        <h1 className="text-3xl font-bold tracking-tight mt-3">{food.name}</h1>
        <p className="text-muted-foreground tabular-nums">pro 100 g · {formatNumber(food.kcal ?? 0)} kcal · P {per100g("protein")} · K {per100g("carbohydrates")} · F {per100g("fat")}</p>

        <h2 className="text-sm uppercase tracking-wide text-muted-foreground mt-5 mb-2">Portion</h2>
        <div className="flex flex-wrap gap-2">
            <PortionPill selected={selectedPortion === null} onClick={() => selectPortion(null)}>Gramm</PortionPill>
            {food.portions.map((portion) =>
                <PortionPill key={portion.id} selected={selectedPortion?.id === portion.id} onClick={() => selectPortion(portion)}>
                    {portion.label} <span className={selectedPortion?.id === portion.id ? "text-white/80" : "text-muted-foreground"}>{formatNumber(portion.grams)} g</span>
                </PortionPill>
            )}
            {!showPortionForm &&
                <button className="rounded-full border-2 border-blue-500 text-blue-500 hover:bg-blue-500/10 px-4 py-1.5 flex items-center gap-x-1" onClick={() => setShowPortionForm(true)}>
                    <Plus className="size-4" />Portion
                </button>
            }
        </div>
        {showPortionForm && <NewPortionForm getFoodId={importFood} kcal={food.kcal ?? 0} onAdded={portionAdded} onCancel={() => setShowPortionForm(false)} />}

        <h2 className="text-sm uppercase tracking-wide text-muted-foreground mt-5 mb-2">Menge</h2>
        <div className="flex items-center gap-x-4">
            <div className="flex items-center bg-accent rounded-2xl px-3 py-2 gap-x-3">
                <button aria-label="Weniger" className="text-blue-500 disabled:opacity-40" disabled={trackFood.amount <= 0} onClick={() => setAmount(trackFood.amount - step)}><Minus className="size-5" /></button>
                <input
                    aria-label="Menge"
                    className="w-16 bg-transparent text-right text-2xl font-semibold tabular-nums outline-none"
                    type="number"
                    min={0}
                    value={trackFood.amount === 0 ? "" : trackFood.amount}
                    onChange={(e) => setAmount(e.target.value === "" ? 0 : parseFloat(e.target.value))}
                />
                <span className="text-xl text-muted-foreground min-w-6">{unit}</span>
                <button aria-label="Mehr" className="text-blue-500" onClick={() => setAmount(trackFood.amount + step)}><Plus className="size-5" /></button>
            </div>
            <span className="text-xl text-muted-foreground tabular-nums">= {formatNumber(grams)} g</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 rounded-2xl bg-muted/60 p-5 mt-5">
            {NUTRIENTS.map((n) =>
                <div key={n.key}>
                    <p className="text-sm text-muted-foreground">{n.label}</p>
                    <p className="text-2xl font-bold tabular-nums">{formatNumber(getNutritionForAmount(withAmount, n.key))}{n.unit}</p>
                </div>
            )}
        </div>

        <div className="flex items-center gap-x-3 border-t pt-4 mt-6">
            <div className="mr-auto">
                <p className="text-sm text-muted-foreground tabular-nums">{formatNumber(grams)} g</p>
                <p className="text-xl font-bold tabular-nums">{formatNumber(getNutritionForAmount(withAmount, "kcal"))} kcal</p>
            </div>
            {!creating &&
                <button aria-label="Eintrag löschen" className="size-10 rounded-full bg-muted text-muted-foreground flex items-center justify-center hover:bg-destructive/15 hover:text-destructive disabled:opacity-50" disabled={saving} onClick={handleDeleteSelectedFood}>
                    <Trash2 className="size-4" />
                </button>
            }
            <button
                className="rounded-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-6 py-2.5 disabled:opacity-50 flex items-center gap-x-2"
                disabled={saving || trackFood.amount <= 0}
                onClick={save}
            >
                {saving && <Spinner className="size-4" />}{creating ? `Zu ${mealLabel} hinzufügen` : "Speichern"}
            </button>
        </div>
    </div>
}

function PortionPill({ selected, onClick, children }: { selected: boolean, onClick: () => void, children: React.ReactNode }) {
    return <button
        className={`rounded-full px-4 py-1.5 border-2 ${selected ? "bg-blue-500 border-blue-500 text-white" : "bg-accent border-accent hover:bg-accent/70"}`}
        onClick={onClick}
    >
        {children}
    </button>
}

function NewPortionForm({ getFoodId, kcal, onAdded, onCancel }: { getFoodId: () => Promise<number>, kcal: number, onAdded: (portions: FoodPortion[], label: string) => void, onCancel: () => void }) {
    const [label, setLabel] = useState("")
    const [grams, setGrams] = useState(100)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const save = async () => {
        setSaving(true)
        setError(null)
        try {
            onAdded(await addFoodPortion(await getFoodId(), { label: label.trim(), grams, isDefault: false }), label)
        } catch {
            setError("Portion konnte nicht gespeichert werden")
            setSaving(false)
        }
    }

    return <div className="rounded-2xl bg-accent p-4 mt-3 flex flex-col gap-y-3">
        <p className="font-semibold">Neue Portion</p>
        <div className="flex flex-wrap items-center gap-2">
            <input
                autoFocus
                className="flex-1 min-w-32 rounded-lg bg-background px-3 py-1.5 outline-none"
                placeholder="z. B. Stück, Scheibe, Tasse"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter" && label.trim() && grams > 0) save() }}
            />
            <div className="flex items-center rounded-lg bg-background px-3 py-1.5 gap-x-1">
                <input
                    aria-label="Gramm pro Portion"
                    className="w-14 bg-transparent text-right font-semibold tabular-nums outline-none"
                    type="number"
                    min={1}
                    value={grams === 0 ? "" : grams}
                    onChange={(e) => setGrams(e.target.value === "" ? 0 : Math.max(0, parseFloat(e.target.value)))}
                />
                <span className="text-muted-foreground">g</span>
            </div>
        </div>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <div className="flex items-center justify-between gap-x-2">
            <p className="text-xs text-muted-foreground tabular-nums">1 {label.trim() || "Portion"} = {formatNumber(grams)} g · {formatNumber(kcal * grams / 100)} kcal</p>
            <div className="flex gap-x-2">
                <button className="rounded-full px-4 py-1.5 text-muted-foreground hover:bg-background/60" onClick={onCancel}>Abbrechen</button>
                <button
                    className="rounded-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-4 py-1.5 disabled:opacity-50 flex items-center gap-x-2"
                    disabled={saving || !label.trim() || grams <= 0}
                    onClick={save}
                >
                    {saving && <Spinner className="size-4" />}Speichern
                </button>
            </div>
        </div>
    </div>
}
