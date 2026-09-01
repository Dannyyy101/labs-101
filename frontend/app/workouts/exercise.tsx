import {
    Card,
    CardAction,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import {
    InputGroup,
    InputGroupAddon,
    InputGroupButton,
    InputGroupInput,
    InputGroupText,
    InputGroupTextarea,
} from "@/components/ui/input-group"
import { Exercise, StrengthTraining, Set as StrengthSet, ExerciseTraining } from "@/utils/types/workoutTypes"
import { SearchIcon, Trash } from "lucide-react"
import { useState } from "react"
import { getAllExercises } from "../exercises/action"
import { Button } from "@/components/ui/button"
import { WorkoutState } from "@/lib/zustand/workoutStore"

export default function ExerciseModal({ exercise, store }: { exercise: ExerciseTraining, store: WorkoutState }) {
    const { setExercises, exercises, updateExercise } = store

    const removeExercise = () => {
        setExercises(exercises.filter((e) => e.id !== exercise.id))
    }

    function StrengthSettings({
        exercise,
        onChange,
    }: {
        exercise: StrengthTraining
        onChange: (exercise: StrengthTraining) => void
    }) {
        function updateSet<K extends keyof StrengthSet>(index: number, key: K, value: StrengthSet[K]) {
            onChange({
                ...exercise,
                sets: exercise.sets.map((s, i) => (i === index ? { ...s, [key]: value } : s)),
            })
        }

        return (
            <div className="px-4">
                <div className="flex">
                    <p className="w-full">Count</p>
                    <p className="w-full">Weight</p>
                    <p className="w-4" />
                </div>
                {exercise.sets.map((set, index) => (
                    <div key={index} className="flex bg-accent rounded-2xl">
                        <input
                            type="number"
                            value={set.count}
                            onChange={(e) => updateSet(index, "count", Number(e.target.value) || 0)}
                            className="w-full pl-2 border-transparent border-2 focus:border-accent outline-0 rounded-2xl"
                        />
                        <input
                            type="number"
                            value={set.weight}
                            onChange={(e) => updateSet(index, "weight", Number(e.target.value) || 0)}
                            className="w-full pl-2 outline-0 rounded-2xl"
                        />
                        <p className="w-4">{set.unit}</p>
                    </div>
                ))}
            </div>
        )
    }

    return <Card className="max-w-96">

        <CardHeader>
            <CardTitle>{exercise.name}</CardTitle>
            <CardDescription>{exercise.description}</CardDescription>
            <CardAction>
                <Button onClick={removeExercise} variant="link"><Trash /></Button>
            </CardAction>
        </CardHeader>
        {exercise.type === "Strength Training" && (
            <StrengthSettings exercise={{ ...exercise, sets: [] }} onChange={updateExercise} />
        )}
    </Card >
}