'use client'

import {
    Drawer,
    DrawerClose,
    DrawerContent,
    DrawerFooter,
    DrawerHeader,
    DrawerTitle,
    DrawerTrigger,
} from "@/components/ui/drawer"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Exercise, ExerciseTypes } from "@/utils/types/workoutTypes"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { createExercise, deleteExercise, updateExercise } from "./action"
import { TrashIcon, X } from "lucide-react"
import Body, { ExtendedBodyPart, Slug } from "react-muscle-highlighter"
import { getNewSlugColor, MUSCLE_LEVELS } from "@/lib/muscle-highlighter/muscle-highlighter"
import { useIsMobile } from "@/hooks/use-mobile"
import { cn } from "@/lib/utils"
import { MUSCLES } from "../workouts/stats"

const EXERCISE_TYPES: ExerciseTypes[] = ["Strength Training", "Swimming", "Running", "Stretching"]

const EditExercise: React.FC<{ exercise: Exercise, children: React.ReactNode, className?: string }> = ({ exercise: oldExercise, children, className }) => {
    const [exercise, setExercise] = useState<Exercise>({ ...oldExercise })
    // a sheet from the bottom on phones, from the side on larger screens
    const isMobile = useIsMobile()

    const isEditing = exercise.id !== -1

    const handleSubmitExercise = async () => {
        if (isEditing) {
            await updateExercise(exercise)
        } else {
            const { id, ...rest } = exercise
            await createExercise(rest)
        }
    }

    const handleDelete = async () => {
        if (confirm(`Delete „${oldExercise.name}“?`)) await deleteExercise(exercise.id)
    }

    // starts with the saved exercise each time, a new one empty again
    const onOpenChange = (open: boolean) => {
        if (open) setExercise({ ...oldExercise })
    }

    return <Drawer swipeDirection={isMobile ? "down" : "right"} onOpenChange={onOpenChange}>
        <DrawerTrigger className={className} render={<Button variant="outline" />}>{children}</DrawerTrigger>
        <DrawerContent className="data-[swipe-axis=x]:sm:[--drawer-content-width:30rem]!">
            <DrawerHeader className="flex-row items-center gap-2 pb-2">
                <DrawerTitle className="flex-1 text-lg font-semibold">{isEditing ? "Update exercise" : "Create exercise"}</DrawerTitle>
                {isEditing && <DrawerClose render={<Button onClick={handleDelete} variant="outline" size="icon" aria-label="Delete exercise" />}>
                    <TrashIcon />
                </DrawerClose>}
            </DrawerHeader>
            <form className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 pb-4" onSubmit={(e) => e.preventDefault()}>
                <div className="flex flex-col gap-4">
                    <div className="-mx-4 flex gap-1.5 overflow-x-auto px-4 [scrollbar-width:none]">
                        {EXERCISE_TYPES.map((type) => <button type="button" key={type}
                            onClick={() => setExercise((prev) => ({ ...prev, type: type }))}
                            className={cn("flex-none rounded-full border px-3 py-1.5 text-sm font-medium",
                                exercise.type === type ? "border-transparent bg-primary text-primary-foreground" : "text-muted-foreground")}>
                            {type}
                        </button>)}
                    </div>
                    <Field>
                        <FieldLabel htmlFor="exercise-name">Name</FieldLabel>
                        <Input
                            id="exercise-name"
                            name="exercise-name"
                            type="text"
                            className="h-10 text-base md:h-9 md:text-sm"
                            value={exercise.name}
                            onChange={(e) => setExercise((prev) => ({ ...prev, name: e.target.value }))}
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="exercise-description">Description</FieldLabel>
                        <Textarea
                            id="exercise-description"
                            name="exercise-description"
                            className="text-base md:text-sm"
                            value={exercise.description}
                            onChange={(e) => setExercise((prev) => ({ ...prev, description: e.target.value }))} />
                    </Field>
                    <MusclePicker parts={exercise.bodyParts} setBodyParts={(parts) => setExercise((prev) => ({ ...prev, bodyParts: parts }))} />
                </div>
            </form>
            <DrawerFooter className="flex-row border-t pt-3 pb-[max(1rem,env(safe-area-inset-bottom))]">
                <DrawerClose render={<Button variant="outline" className="h-10 flex-1" />}>Cancel</DrawerClose>
                <DrawerClose render={<Button onClick={handleSubmitExercise} disabled={!exercise.name.trim()} className="h-10 flex-1" />}>
                    {isEditing ? "Save" : "Create"}
                </DrawerClose>
            </DrawerFooter>
        </DrawerContent>
    </Drawer>
}

/**
 * Front and back of a body, tapping a muscle cycles how strongly the exercise works it
 * (a little, clearly, main muscle) and then removes it again.
 */
function MusclePicker({ parts, setBodyParts }: { parts: ExtendedBodyPart[], setBodyParts: (parts: ExtendedBodyPart[]) => void }) {
    // a muscle stored for both sides is shown once, with its strongest intensity
    const bySlug = new Map<Slug, ExtendedBodyPart>()
    parts.forEach((part) => {
        if (!part.slug) return
        const current = bySlug.get(part.slug)
        if (!current || (part.intensity ?? 0) > (current.intensity ?? 0)) bySlug.set(part.slug, part)
    })
    const selected = [...bySlug.values()].sort((a, b) => (b.intensity ?? 0) - (a.intensity ?? 0))
    // colored by intensity (older exercises have other colors stored), always on both sides
    const data = selected.map(({ color, side, ...part }) => part)

    const setIntensity = (slug: Slug, intensity: number) => {
        const rest = parts.filter((part) => part.slug !== slug)
        if (intensity < 1 || intensity > MUSCLE_LEVELS.length) return setBodyParts(rest)
        // keeps the stored row (and its id), so the backend updates instead of replacing it
        setBodyParts([...rest, { ...bySlug.get(slug), slug, intensity, color: getNewSlugColor(intensity - 1), side: undefined }])
    }
    const onPress = (slug?: Slug) => {
        if (slug) setIntensity(slug, (bySlug.get(slug)?.intensity ?? 0) + 1)
    }

    return <div className="flex flex-col gap-3">
        <div className="flex items-baseline justify-between gap-2">
            <span className="text-sm font-medium">Muscles</span>
            <span className="text-xs text-muted-foreground">Tap to cycle intensity</span>
        </div>
        <div className="grid grid-cols-2 gap-2 rounded-xl bg-muted/50 p-3">
            {(["front", "back"] as const).map((side) => <div key={side} className="flex flex-col items-center gap-1">
                <div className="w-full max-w-44 [&_svg]:h-auto [&_svg]:w-full">
                    <Body data={data} side={side} gender="male" colors={MUSCLE_LEVELS} defaultFill="#8e8e9340" border="none"
                        onBodyPartPress={(part) => onPress(part.slug)} />
                </div>
                <span className="text-xs font-medium text-muted-foreground">{side === "front" ? "Front" : "Back"}</span>
            </div>)}
        </div>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
            {["Supporting", "Secondary", "Primary"].map((label, i) => <span key={label} className="flex items-center gap-1.5">
                <span className="size-2.5 rounded-full" style={{ background: MUSCLE_LEVELS[i] }} />{label}
            </span>)}
        </div>
        {selected.length > 0 && <div className="flex flex-wrap gap-1.5">
            {selected.map((part) => <button type="button" key={part.slug} onClick={() => setIntensity(part.slug!, 0)}
                aria-label={`Remove ${MUSCLES[part.slug!] ?? part.slug}`}
                className={cn("flex items-center gap-1 rounded-full py-1 pr-1.5 pl-2.5 text-xs font-medium",
                    (part.intensity ?? 0) >= MUSCLE_LEVELS.length
                        ? "bg-[#ff9f0a]/15 text-[#c76f00] dark:text-[#ff9f0a]"
                        : "bg-muted text-muted-foreground")}>
                {MUSCLES[part.slug!] ?? part.slug}<X className="size-3.5" />
            </button>)}
        </div>}
    </div>
}

export default EditExercise;
