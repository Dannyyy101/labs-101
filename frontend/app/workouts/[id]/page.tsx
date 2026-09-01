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
import { ReactNode, useState } from "react";
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


export default function WorkoutPage() {
    const params = useParams<{ id: string }>()
    const workoutId = parseInt(params.id)

    const isCreating = workoutId === -1

    const store = useExercisesStore()
    const { exercises, setExercises } = store
    const map = new Map<string, ExtendedBodyPart>()
    exercises.forEach((exercise) => exercise.bodyParts.forEach((part) => {
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

    return <div className="w-full flex gap-32 mt-10 relative">

        <BodyModal bodyData={[...map.values()]} />
        <section className="w-full relative">
            {exercises.length === 0 ? <EmptyExercises workoutState={store} /> : <div className="flex flex-col gap-4 overflow-y-auto p-2">
                {exercises.map((exercise, index) => <ExerciseModal store={store} exercise={exercise} key={index} />)}</div>}
        </section>
        <div className="mr-8">
            {exercises.length > 0 &&
                <AddExerciseDialog className="w-32 border-accent shadow border px-2 py-1 rounded-2xl" workoutState={store}>Add Exercise</AddExerciseDialog>
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

    function changeType(prev: Exercise, type: ExerciseTypes): ExerciseTraining {
        if (prev.type === type) return prev as ExerciseTraining

        const base = {
            id: prev.id,
            name: prev.name,
            description: prev.description,
            bodyParts: prev.bodyParts,
        }

        switch (type) {
            case "Strength Training":
                return { ...base, type, sets: [] }
            case "Swimming":
                return { ...base, type /* , swimming-specific defaults */ }
            case "Running":
                return { ...base, type /* , running-specific defaults */ }
            case "Stretching":
                return { ...base, type /* , stretching-specific defaults */ }
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
                    {foundExercises.map((found) => <button onClick={() => setExercises([...exercises, changeType(found, found.type)])} className="text-left hover:bg-accent p-1 pl-2 rounded-2xl" key={found.id}>{found.name}</button>)}
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