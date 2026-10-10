'use client'

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Spinner } from "@/components/ui/spinner"
import { DEFAULT_CALORIE_GOAL, UserSettings } from "@/utils/types/settings"
import { AlertCircle, Check } from "lucide-react"
import { useState, useTransition } from "react"
import { saveSettings } from "./action"

export default function SettingsForm({ settings }: { settings: UserSettings | null }) {
    const savedGoal = settings?.calorieGoal ?? DEFAULT_CALORIE_GOAL
    const [calorieGoal, setCalorieGoal] = useState(String(savedGoal))
    const [error, setError] = useState<string | null>(null)
    const [saved, setSaved] = useState(false)
    const [saving, startSaving] = useTransition()

    const goal = Number(calorieGoal)
    const valid = Number.isInteger(goal) && goal > 0
    const changed = goal !== savedGoal

    const save = () => startSaving(async () => {
        setError(null)
        const result = await saveSettings({ calorieGoal: goal })
        if (result.ok) setSaved(true)
        else setError(result.error)
    })

    return <form
        className="rounded-3xl bg-card shadow-sm p-6 flex flex-col gap-5"
        onSubmit={(e) => { e.preventDefault(); if (valid && changed) save() }}
    >
        <h2 className="text-xl font-semibold">Ernährung</h2>

        <Field data-invalid={!valid}>
            <FieldLabel htmlFor="calorie-goal">Kalorienziel</FieldLabel>
            <InputGroup className="max-w-60">
                <InputGroupInput
                    id="calorie-goal"
                    type="number"
                    inputMode="numeric"
                    min={1}
                    step={1}
                    value={calorieGoal}
                    aria-invalid={!valid}
                    onChange={(e) => { setCalorieGoal(e.target.value); setSaved(false) }}
                />
                <InputGroupAddon align="inline-end">kcal / Tag</InputGroupAddon>
            </InputGroup>
            <FieldDescription>
                {valid ? "Wird beim Food Tracking als Tagesziel angezeigt." : "Bitte eine ganze Zahl größer als 0 eingeben."}
            </FieldDescription>
        </Field>

        {error && (
            <Alert variant="destructive">
                <AlertCircle className="size-4" />
                <AlertTitle>Fehler</AlertTitle>
                <AlertDescription>{error}</AlertDescription>
            </Alert>
        )}

        <div className="flex items-center gap-x-3">
            <Button type="submit" disabled={!valid || !changed || saving}>
                {saving && <Spinner />}Speichern
            </Button>
            {saved && !changed && <span className="flex items-center gap-x-1 text-sm text-muted-foreground"><Check className="size-4" />Gespeichert</span>}
        </div>
    </form>
}
