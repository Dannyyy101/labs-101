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
import { useState } from "react";
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
import { Exercise } from "@/utils/types/workoutTypes";
import { getAllExercises } from "@/app/exercises/action";


export default function WorkoutPage() {
    const params = useParams<{ id: string }>()
    const workoutId = parseInt(params.id)

    const isCreating = workoutId === -1

    const store = useExercisesStore()
    const { exercises, setExercises } = store

    return <div className="w-full flex gap-32">
        <BodyModal />
        <section className="w-full relative">
            {exercises.length === 0 ? <EmptyExercises workoutState={store} /> : <>
                {exercises.map((exercise, index) => <ExerciseModal exercise={exercise} key={index} />)}</>}
        </section>
    </div>
}




export function EmptyExercises({ workoutState }: { workoutState: WorkoutState }) {
    const [foundExercises, setFoundExercises] = useState<Exercise[]>([])
    const [searchInput, setSearchInput] = useState<string>("")

    const onSearchInputChange = async (value: string) => {
        setSearchInput(value)
        if (value.length === 0) {
            setFoundExercises([])
            return
        }
        const exercises = await getAllExercises(value)
        setFoundExercises(exercises)
    }
    const { setExercises } = workoutState

    const addEmptyExercise = () => {
        setExercises([{ id: -1, name: "", description: "", type: "EMPTY", bodyParts: [] }])
    }
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
                <Dialog>
                    <DialogTrigger>Open</DialogTrigger>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Are you absolutely sure?</DialogTitle>
                            <InputGroup className="max-w-xs">
                                <InputGroupInput value={searchInput} onChange={(e) => onSearchInputChange(e.target.value)} placeholder="Search..." />
                                <InputGroupAddon>
                                    <Search />
                                </InputGroupAddon>
                                <InputGroupAddon align="inline-end">{foundExercises.length} results</InputGroupAddon>
                            </InputGroup>
                            <div className="overflow-y-auto flex flex-col max-h-20">
                                {foundExercises.map((found) => <button className="text-left hover:bg-accent p-1 pl-2 rounded-2xl" key={found.id}>{found.name}</button>)}
                            </div>
                        </DialogHeader>
                    </DialogContent>
                </Dialog>
            </EmptyContent>
        </Empty>
    )
}


function BodyModal() {
    const bodyData: readonly ExtendedBodyPart[] = [

    ] as const;
    return <div className="ml-32"> <Body
        data={bodyData}
        side="front"
        gender="male"
        scale={1.5}
        onBodyPartPress={(part) => console.log("Clicked:", part.slug)}
    /></div>
}