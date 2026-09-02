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
import { WorkoutExercise, StrengthExercise, ExerciseSet, WorkoutExerciseType } from "@/utils/types/types"

export default function ExerciseModal({ exercise, store }: { exercise: WorkoutExercise, store: WorkoutState }) {
    const { setExercises, exercises, updateExercise } = store

    const removeExercise = () => {
        setExercises(exercises.filter((e) => e.exercise.id !== exercise.exercise.id))
    }

    function StrengthSettings({
        exercise,
        onChange,
    }: {
        exercise: StrengthExercise
        onChange: (exercise: StrengthExercise) => void
    }) {
        function updateSet<K extends keyof ExerciseSet>(index: number, key: K, value: ExerciseSet[K]) {
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
                    <div key={index} className="flex bg-accent rounded-2xl my-1">
                        <input
                            type="number"
                            value={set.reps}
                            onChange={(e) => updateSet(index, "reps", Number(e.target.value) || 0)}
                            className="w-full pl-2 border-transparent border-2 focus:border-accent outline-0 rounded-2xl"
                        />
                        <input
                            type="number"
                            value={set.weightKg}
                            onChange={(e) => updateSet(index, "weightKg", Number(e.target.value) || 0)}
                            className="w-full pl-2 outline-0 rounded-2xl"
                        />
                        <p className="w-4">{"kg"}</p>
                    </div>
                ))}
                <button className="text-left w-fit text-xs hover:underline mt-2 ml-1" onClick={() => onChange({ ...exercise, sets: [...exercise.sets, { reps: 10, weightKg: 30, rpe: 10, order: 0 }] })}>Add more</button>
            </div>
        )
    }
    console.log(exercise)

    return <Card className="max-w-96">

        <CardHeader>
            <CardTitle>{exercise.exercise.name}</CardTitle>
            <CardDescription>{exercise.exercise.description}</CardDescription>
            <CardAction>
                <Button onClick={removeExercise} variant="link"><Trash /></Button>
            </CardAction>
        </CardHeader>
        {exercise.type === WorkoutExerciseType.STRENGTH_EXERCISE && (
            <StrengthSettings exercise={{ ...exercise, sets: exercise.sets ?? [] }} onChange={updateExercise} />
        )}
    </Card >
}