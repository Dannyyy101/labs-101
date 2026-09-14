'use client'

import { useEffect, useMemo, useState } from "react"
import { Plus, Star, Trash2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
    Drawer,
    DrawerClose,
    DrawerContent,
    DrawerDescription,
    DrawerFooter,
    DrawerHeader,
    DrawerTitle,
} from "@/components/ui/drawer"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Food, FoodPortion, FoodWithPortion } from "@/utils/types/food"
import { createFood, deleteFood, updateFood } from "./action"

type PortionRow = Omit<FoodPortion, "id"> & { id: number; key: string }

const EMPTY_FOOD: FoodWithPortion = {
    id: -1,
    blsCode: "",
    name: "",
    kcal: null,
    water: null,
    protein: null,
    fat: null,
    carbohydrates: null,
    fiber: null,
    portions: [],
}

const parseNumber = (value: string): number | null => {
    if (value.trim() === "") return null
    const parsed = Number(value.replace(",", "."))
    return Number.isFinite(parsed) ? parsed : null
}

const toInput = (value: number | null | undefined) =>
    value === null || value === undefined ? "" : String(value)

const newKey = () =>
    typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : Math.random().toString(36).slice(2)

const NutrientInput: React.FC<{
    label: string
    unit: string
    value: number | null | undefined
    onValueChange: (value: number | null) => void
    step?: number
}> = ({ label, unit, value, onValueChange, step = 0.1 }) => (
    <Field className="gap-0">
        <FieldLabel className="flex items-center gap-1.5 text-muted-foreground">
            {label}
        </FieldLabel>
        <div className="relative">
            <Input
                type="number"
                inputMode="decimal"
                min={0}
                step={step}
                placeholder="0"
                value={toInput(value)}
                onChange={(e) => onValueChange(parseNumber(e.target.value))}
                className="pr-12 tabular-nums"
            />
            <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs text-muted-foreground">
                {unit}
            </span>
        </div>
    </Field>
)

const MacroSplit: React.FC<{
    protein: number | null | undefined
    carbohydrates: number | null | undefined
    fat: number | null | undefined
}> = ({ protein, carbohydrates, fat }) => {
    const proteinKcal = (protein ?? 0) * 4
    const carbKcal = (carbohydrates ?? 0) * 4
    const fatKcal = (fat ?? 0) * 9
    const total = proteinKcal + carbKcal + fatKcal

    if (total <= 0) {
        return (
            <p className="text-xs text-muted-foreground">
                Add protein, carbs or fat to see the energy split.
            </p>
        )
    }

    const share = (value: number) => Math.round((value / total) * 100)

    return (
        <div className="space-y-2 mt-2">
            <div className="flex h-2 overflow-hidden rounded-full bg-muted">
                <div className="bg-protein" style={{ width: `${(proteinKcal / total) * 100}%` }} />
                <div className="bg-carbohydrates" style={{ width: `${(carbKcal / total) * 100}%` }} />
                <div className="bg-fat" style={{ width: `${(fatKcal / total) * 100}%` }} />
            </div>
            <div className="flex gap-4 text-xs text-muted-foreground tabular-nums">
                <span>Protein {share(proteinKcal)}%</span>
                <span>Carbs {share(carbKcal)}%</span>
                <span>Fat {share(fatKcal)}%</span>
            </div>
        </div>
    )
}


const EditFood: React.FC<{
    food?: FoodWithPortion
    close: () => void
    onSaved?: (food: FoodWithPortion) => void
    className?: string
}> = ({ food: oldFood, close, onSaved }) => {
    const [food, setFood] = useState<FoodWithPortion>(oldFood ?? EMPTY_FOOD)
    const [portions, setPortions] = useState<PortionRow[]>([])
    const [saving, setSaving] = useState(false)
    const [confirmDelete, setConfirmDelete] = useState(false)

    useEffect(() => {
        const next = oldFood ?? EMPTY_FOOD
        setFood({ ...next })
        setPortions((next.portions ?? []).map((p) => ({ ...p, key: newKey() })))
        setConfirmDelete(false)
    }, [oldFood])

    const isEditing = !!oldFood && food.id !== -1
    const isOpen = !!oldFood

    const set = <K extends keyof Food>(key: K, value: Food[K]) =>
        setFood((prev) => ({ ...prev, [key]: value }))

    const computedKcal = useMemo(
        () =>
            Math.round(
                (food.protein ?? 0) * 4 + (food.carbohydrates ?? 0) * 4 + (food.fat ?? 0) * 9,
            ),
        [food.protein, food.carbohydrates, food.fat],
    )

    const kcalMismatch =
        computedKcal > 0 && food.kcal !== null && Math.abs(computedKcal - (food.kcal ?? 0)) > 15

    const nameError = food.name.trim() === "" ? "Every food needs a name." : null
    const portionError = portions.some((p) => !p.grams || p.grams <= 0)
        ? "Portions need a weight in grams."
        : null
    const canSave = !nameError && !portionError && !saving


    const addPortion = () =>
        setPortions((prev) => [
            ...prev,
            {
                key: newKey(),
                id: -1,
                label: "",
                grams: null as unknown as number,
                isDefault: prev.length === 0,
            },
        ])

    const updatePortion = (key: string, patch: Partial<PortionRow>) =>
        setPortions((prev) => prev.map((p) => (p.key === key ? { ...p, ...patch } : p)))

    const markDefault = (key: string) =>
        setPortions((prev) => prev.map((p) => ({ ...p, isDefault: p.key === key })))

    const removePortion = (key: string) =>
        setPortions((prev) => {
            const next = prev.filter((p) => p.key !== key)
            if (next.length > 0 && !next.some((p) => p.isDefault)) next[0].isDefault = true
            return next
        })


    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault()
        if (!canSave) return

        setSaving(true)
        try {
            const payload: FoodWithPortion = {
                ...food,
                name: food.name.trim(),
                blsCode: food.blsCode?.trim() || "",
                portions: portions.map(({ key, id, ...rest }) => ({
                    ...rest,
                    ...(id > 0 ? { id } : {}),
                })) as FoodPortion[],
            }

            const saved = isEditing
                ? await updateFood(payload)
                : await createFood((({ id, ...rest }) => rest)(payload))

            onSaved?.(saved ?? payload)
            close()
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async () => {
        if (!isEditing) return
        if (!confirmDelete) {
            setConfirmDelete(true)
            return
        }
        setSaving(true)
        try {
            await deleteFood(food.id)
            close()
        } finally {
            setSaving(false)
        }
    }

    return (
        <Drawer open={isOpen} onOpenChange={(open) => !open && close()} swipeDirection="right">
            <DrawerContent className="flex h-full min-w-120 flex-col">
                <DrawerHeader className="border-b">
                    <DrawerTitle>{isEditing ? "Edit food" : "New food"}</DrawerTitle>
                    <DrawerDescription>
                        All nutrition values refer to 100 g of the raw product.
                    </DrawerDescription>
                    {isEditing && (
                        <Button
                            type="button"
                            onClick={handleDelete}
                            className="absolute top-4 right-4"
                            variant={confirmDelete ? "destructive" : "outline"}
                            size={confirmDelete ? "default" : "icon"}
                            aria-label="Delete food"
                        >
                            <Trash2 />
                            {confirmDelete && "Delete for good"}
                        </Button>
                    )}
                </DrawerHeader>

                <form onSubmit={handleSubmit} className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1 space-y-8 overflow-y-auto p-4">
                        {/* identity ------------------------------------------------ */}
                        <section className="space-y-4">
                            <Field>
                                <FieldLabel htmlFor="food-name">Name</FieldLabel>
                                <Input
                                    id="food-name"
                                    name="name"
                                    autoFocus
                                    placeholder="Rolled oats"
                                    value={food.name}
                                    onChange={(e) => set("name", e.target.value)}
                                    aria-invalid={!!nameError}
                                />
                                {nameError && (
                                    <p className="text-xs text-destructive">{nameError}</p>
                                )}
                            </Field>

                            <Field>
                                <FieldLabel htmlFor="food-bls">BLS code</FieldLabel>
                                <Input
                                    id="food-bls"
                                    name="blsCode"
                                    placeholder="C1000000"
                                    value={food.blsCode ?? ""}
                                    onChange={(e) => set("blsCode", e.target.value)}
                                    className="font-mono uppercase"
                                />
                                <p className="text-xs text-muted-foreground">
                                    Optional — links this food to the Bundeslebensmittelschlüssel.
                                </p>
                            </Field>
                        </section>

                        {/* nutrition ----------------------------------------------- */}
                        <section className="space-y-4">
                            <div className="flex items-baseline justify-between">
                                <h3 className="text-sm font-medium">Nutrition per 100 g</h3>
                                {computedKcal > 0 && (
                                    <button
                                        type="button"
                                        onClick={() => set("kcal", computedKcal)}
                                        className="text-xs text-muted-foreground underline-offset-4 hover:underline"
                                    >
                                        Use {computedKcal} kcal from macros
                                    </button>
                                )}
                            </div>

                            <NutrientInput
                                label="Energy"
                                unit="kcal"
                                step={1}
                                value={food.kcal}
                                onValueChange={(v) => set("kcal", v)}
                            />

                            <div className="grid grid-cols-2 gap-4">
                                <NutrientInput
                                    label="Protein"
                                    unit="g"
                                    value={food.protein}
                                    onValueChange={(v) => set("protein", v)}
                                />
                                <NutrientInput
                                    label="Carbohydrates"
                                    unit="g"
                                    value={food.carbohydrates}
                                    onValueChange={(v) => set("carbohydrates", v)}
                                />
                                <NutrientInput
                                    label="Fat"
                                    unit="g"
                                    value={food.fat}
                                    onValueChange={(v) => set("fat", v)}
                                />
                                <NutrientInput
                                    label="Fiber"
                                    unit="g"
                                    value={food.fiber}
                                    onValueChange={(v) => set("fiber", v)}
                                />
                                <NutrientInput
                                    label="Water"
                                    unit="g"
                                    value={food.water}
                                    onValueChange={(v) => set("water", v)}
                                />
                            </div>

                            <MacroSplit
                                protein={food.protein}
                                carbohydrates={food.carbohydrates}
                                fat={food.fat}
                            />

                            {kcalMismatch && (
                                <p className="text-xs text-amber-600">
                                    Macros add up to {computedKcal} kcal — check the entered energy.
                                </p>
                            )}
                        </section>

                        {/* portions ------------------------------------------------ */}
                        <section className="space-y-3">
                            <div className="flex items-baseline justify-between">
                                <h3 className="text-sm font-medium">Portions</h3>
                                <span className="text-xs text-muted-foreground">
                                    Star marks the one preselected when logging
                                </span>
                            </div>

                            {portions.length === 0 ? (
                                <p className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">
                                    No portions yet. Add one like “slice” or “bowl” so you don’t
                                    have to weigh every time.
                                </p>
                            ) : (
                                <ul className="space-y-2 flex flex-col gap-y-1">
                                    {portions.map((portion) => (
                                        <li key={portion.key} className="flex items-end gap-2">
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon"
                                                aria-label="Set as default portion"
                                                aria-pressed={portion.isDefault}
                                                onClick={() => markDefault(portion.key)}
                                                className={
                                                    portion.isDefault
                                                        ? "text-amber-500"
                                                        : "text-muted-foreground"
                                                }
                                            >
                                                <Star
                                                    fill={
                                                        portion.isDefault ? "currentColor" : "none"
                                                    }
                                                />
                                            </Button>

                                            <Input
                                                placeholder="Slice"
                                                value={portion.label ?? ""}
                                                onChange={(e) =>
                                                    updatePortion(portion.key, {
                                                        label: e.target.value,
                                                    })
                                                }
                                                className="flex-1"
                                            />

                                            <div className="relative w-28">
                                                <Input
                                                    type="number"
                                                    inputMode="decimal"
                                                    min={0}
                                                    step={1}
                                                    placeholder="0"
                                                    value={toInput(portion.grams)}
                                                    onChange={(e) =>
                                                        updatePortion(portion.key, {
                                                            grams: parseNumber(
                                                                e.target.value,
                                                            ) as number,
                                                        })
                                                    }
                                                    className="pr-8 tabular-nums"
                                                />
                                                <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-xs text-muted-foreground">
                                                    g
                                                </span>
                                            </div>

                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon"
                                                aria-label="Remove portion"
                                                onClick={() => removePortion(portion.key)}
                                            >
                                                <Trash2 />
                                            </Button>
                                        </li>
                                    ))}
                                </ul>
                            )}

                            {portionError && (
                                <p className="text-xs text-destructive">{portionError}</p>
                            )}

                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={addPortion}
                                className="w-full mt-2"
                            >
                                <Plus />
                                Add portion
                            </Button>
                        </section>
                    </div>

                    <DrawerFooter className="flex-row gap-2 border-t">
                        <DrawerClose
                            render={
                                <Button
                                    type="button"
                                    variant="outline"
                                    className="flex-1"
                                    onClick={close}
                                />
                            }
                        >
                            Cancel
                        </DrawerClose>
                        <Button type="submit" disabled={!canSave} className="flex-1">
                            {saving ? "Saving…" : isEditing ? "Save changes" : "Create food"}
                        </Button>
                    </DrawerFooter>
                </form>
            </DrawerContent>
        </Drawer>
    )
}

export default EditFood