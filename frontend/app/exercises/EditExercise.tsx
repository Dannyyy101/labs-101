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
import { Badge } from "@/components/ui/badge"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Exercise } from "@/utils/types/workoutTypes"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { createExercise, deleteExercise } from "./action"
import { TrashIcon } from "lucide-react"

const EditExercise: React.FC<{ exercise: Exercise, children: React.ReactNode, className?: string }> = ({ exercise, children, className }) => {
    const isEditing = exercise.id !== -1
    const [selectedExerciseType, setSelectedExerciseType] = useState<string>(exercise.type)
    const exerciseTypes = ["Strength Training", "Swimming", "Running", "Stretching"]

    return <Drawer swipeDirection="right">
        <DrawerTrigger className={className} render={<Button variant="outline" />}>{children}</DrawerTrigger>
        <DrawerContent className={"min-w-120"}>
            <DrawerClose render={<Button onClick={() => deleteExercise(exercise.id)} className="absolute top-4 right-4" variant="outline" size="icon" aria-label="Submit">
                <TrashIcon />
            </Button>}>Submit</DrawerClose>
            <DrawerHeader>
                <DrawerTitle>{isEditing ? "Update exercise" : "Create exercise"}</DrawerTitle>
            </DrawerHeader>
            <form className="relative h-full" action={createExercise}>
                <div className="p-4">
                    <div className="flex flex-col gap-4">
                        <div className="flex flex-wrap gap-1">{exerciseTypes.map((type) => <Badge className="hover:cursor-pointer" variant={selectedExerciseType === type ? "default" : "outline"} onClick={() => setSelectedExerciseType(type)} key={type}>{type}</Badge>)}</div>
                        <input name="exercise-type" hidden value={selectedExerciseType} readOnly />
                        <Field>
                            <FieldLabel htmlFor="input-field-username">Name</FieldLabel>
                            <Input
                                name="exercise-name"
                                type="text"
                                defaultValue={exercise.name}
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="input-field-username">Description</FieldLabel>
                            <Textarea
                                name="exercise-description"
                                defaultValue={exercise.description}
                            />
                        </Field>
                    </div>
                </div>
                <DrawerFooter className="absolute w-full bottom-0">
                    <DrawerClose render={<Button type="submit" />}>Submit</DrawerClose>
                    <DrawerClose render={<Button variant="outline" />}>Cancel</DrawerClose>
                </DrawerFooter>
            </form>
        </DrawerContent>
    </Drawer>
}

export default EditExercise;