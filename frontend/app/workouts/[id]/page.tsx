'use client'
import { useParams } from "next/navigation";
import ExerciseModal from "../exercise";
import Body, { type ExtendedBodyPart } from "react-muscle-highlighter";

import { ArrowUpRightIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyMedia,
    EmptyTitle,
} from "@/components/ui/empty"
import { ReactNode, useEffect, useState } from "react";
import { useExercisesStore, WorkoutState } from "@/lib/zustand/workoutStore";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"

import { Search } from "lucide-react"

import {
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
} from "@/components/ui/input-group"
import { Exercise, ExerciseTraining, ExerciseTypes } from "@/utils/types/workoutTypes";
import { getAllExercises } from "@/app/exercises/action";
import { getNewSlugColor } from "@/lib/muscle-highlighter/muscle-highlighter";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { getWorkoutById } from "../action";
import { BaseWorkoutExercise, WorkoutExercise, WorkoutExerciseType } from "@/utils/types/types";


export default function WorkoutPage() {
    const params = useParams<{ id: string }>()
    const workoutId = parseInt(params.id)

    const isCreating = workoutId === -1

    const store = useExercisesStore()
    const { exercises, setExercises } = store

    useEffect(() => {
        const fetch = async () => {
            const workout = await getWorkoutById(workoutId);
            setExercises(workout.workoutExercises)
        }
        if (!isCreating) {
            fetch()
        }
    }, [])


    const map = new Map<string, ExtendedBodyPart>()
    exercises.forEach((exercise) => exercise.exercise.bodyParts.forEach((part) => {
        if (!part.slug) return
        const element = map.get(part.slug)
        if (element && element.intensity) {
            const newColor = getNewSlugColor(element.intensity + 1)
            if (newColor) {
                map.set(part.slug, { ...part, color: newColor, intensity: element.intensity + 1 })
            }
        } else {
            map.set(part.slug, part)
        }
    }
    ))

    const handleSaveWorkout = async () => {

    }

    return <div className="w-full flex gap-32 mt-10 relative">

        <BodyModal bodyData={[...map.values()]} />
        <section className="w-full relative">
            {exercises.length === 0 ? <EmptyExercises workoutState={store} /> :
                <div className="">
                    <Field>
                        <FieldLabel htmlFor="input-field-username">Name</FieldLabel>
                        <Input
                            id="input-field-name"
                            type="text"
                        />
                    </Field>
                    <div className="flex flex-col gap-4 overflow-y-auto p-2 mt-2">
                        {exercises.map((exercise, index) => <ExerciseModal store={store} exercise={exercise} key={index} />)}
                    </div>
                </div>}
        </section>
        <div className="mr-8 flex gap-x-2">
            <Button className="w-32 h-10" onClick={handleSaveWorkout}>Save Workout</Button>
            {exercises.length > 0 &&
                <AddExerciseDialog className="hover:bg-accent w-32 h-10 border-accent shadow border px-2 py-1 rounded-2xl" workoutState={store}>Add Exercise</AddExerciseDialog>
            }
        </div>
    </div>
}




export function EmptyExercises({ workoutState }: { workoutState: WorkoutState }) {

    return (
        <Empty className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
            <EmptyHeader>
                <EmptyTitle>No Exercises Yet</EmptyTitle>
                <EmptyDescription>
                    You haven&apos;t added any exercises yet. Get started by adding
                    your first exercise.
                </EmptyDescription>
            </EmptyHeader>
            <EmptyContent className="flex-row justify-center gap-2">
                <AddExerciseDialog workoutState={workoutState} >Open</AddExerciseDialog>
            </EmptyContent>
        </Empty>
    )
}

const AddExerciseDialog: React.FC<{ workoutState: WorkoutState, className?: string, children?: ReactNode }> = ({ workoutState, className, children }) => {
    const { setExercises, exercises } = workoutState

    const [foundExercises, setFoundExercises] = useState<Exercise[]>([])
    const [searchInput, setSearchInput] = useState<string>("")

    const onSearchInputChange = async (value: string) => {
        setSearchInput(value)
        if (value.length === 0) {
            setFoundExercises([])
            return
        }
        const exercises = await getAllExercises({ name: value })
        setFoundExercises(exercises)
    }

    function changeType(prev: BaseWorkoutExercise, type: WorkoutExerciseType): WorkoutExercise {
        if (prev.type === type) return prev as WorkoutExercise

        const base = {
            exercise: prev.exercise
        }

        switch (type) {
            case WorkoutExerciseType.STRENGTH_EXERCISE:
                return { ...base, type, sets: [] }
            default:
                throw new Error()
        }

    }

    return <Dialog>
        <DialogTrigger className={className}>{children}</DialogTrigger>
        <DialogContent>
            <DialogHeader>
                <DialogTitle></DialogTitle>
                <InputGroup className="max-w-xs">
                    <InputGroupInput value={searchInput} onChange={(e) => onSearchInputChange(e.target.value)} placeholder="Search..." />
                    <InputGroupAddon>
                        <Search />
                    </InputGroupAddon>
                    <InputGroupAddon align="inline-end">{foundExercises.length} results</InputGroupAddon>
                </InputGroup>
                <div className="overflow-y-auto flex flex-col max-h-20">
                    {foundExercises.map((found) => <button onClick={() => setExercises([...exercises, changeType({ exercise: found, type: WorkoutExerciseType.STRENGTH_EXERCISE }, WorkoutExerciseType.STRENGTH_EXERCISE)])} className="text-left hover:bg-accent p-1 pl-2 rounded-2xl" key={found.id}>{found.name}</button>)}
                </div>
            </DialogHeader>
        </DialogContent>
    </Dialog>
}


function BodyModal({ bodyData }: { bodyData: ExtendedBodyPart[] }) {
    return <div className="ml-32"> <Body
        data={bodyData}
        side="front"
        gender="male"
        scale={1.5}
        onBodyPartPress={(part) => console.log("Clicked:", part.slug)}
    /></div>
}