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
import { Exercise, ExerciseTypes, StrengthTraining } from "@/utils/types/workoutTypes"
import { useEffect, useReducer, useState } from "react"
import { Button } from "@/components/ui/button"
import { createExercise, deleteExercise, updateExercise } from "./action"
import { TrashIcon } from "lucide-react"
import Body, { BodyPart, ExtendedBodyPart, Slug } from "react-muscle-highlighter"
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from "@/components/ui/accordion"
import { getNewSlugColor } from "@/lib/muscle-highlighter/muscle-highlighter"

const EditExercise: React.FC<{ exercise: Exercise, children: React.ReactNode, className?: string }> = ({ exercise: oldExercise, children, className }) => {
    const [exercise, setExercise] = useState<Exercise>({ ...oldExercise })

    const isEditing = exercise.id !== -1
    const exerciseTypes: ExerciseTypes[] = ["Strength Training", "Swimming", "Running", "Stretching"]

    const handleSubmitExercise = async () => {
        if (isEditing) {
            await updateExercise(exercise)
        } else {
            const { id, ...rest } = exercise
            await createExercise(rest)
        }
    }


    return <Drawer swipeDirection="right">
        <DrawerTrigger className={className} render={<Button variant="outline" />}>{children}</DrawerTrigger>
        <DrawerContent className={"min-w-120"}>
            <DrawerClose render={<Button onClick={() => deleteExercise(exercise.id)} className="absolute top-4 right-4" variant="outline" size="icon" aria-label="Submit">
                <TrashIcon />
            </Button>}>Submit</DrawerClose>
            <DrawerHeader>
                <DrawerTitle>{isEditing ? "Update exercise" : "Create exercise"}</DrawerTitle>
            </DrawerHeader>
            <form className="relative h-full overflow-y-auto">
                <div className="p-4">
                    <div className="flex flex-col gap-4">
                        <div className="flex flex-wrap gap-1">{exerciseTypes.map((type) => <Badge className="hover:cursor-pointer" variant={exercise.type === type ? "default" : "outline"} onClick={() => setExercise((prev) => ({ ...prev, type: type }))} key={type}>{type}</Badge>)}</div>
                        <Field>
                            <FieldLabel htmlFor="input-field-username">Name</FieldLabel>
                            <Input
                                name="exercise-name"
                                type="text"
                                value={exercise.name}
                                onChange={(e) => setExercise((prev) => ({ ...prev, name: e.target.value }))}
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="input-field-username">Description</FieldLabel>
                            <Textarea
                                name="exercise-description"
                                value={exercise.description}
                                onChange={(e) => setExercise((prev) => ({ ...prev, description: e.target.value }))} />
                        </Field>
                    </div>

                    <div className="w-full flex justify-center">
                        <BodyModal parts={exercise.bodyParts} setBodyParts={(parts) => setExercise((prev) => ({ ...prev, bodyParts: parts }))} />
                    </div>

                </div>
                <DrawerFooter className="absolute w-full bottom-0">
                    <DrawerClose render={<Button onClick={handleSubmitExercise} />}>Submit</DrawerClose>
                    <DrawerClose render={<Button variant="outline" />}>Cancel</DrawerClose>
                </DrawerFooter>
            </form>
        </DrawerContent>
    </Drawer>
}


function BodyModal({ parts, setBodyParts }: { parts: ExtendedBodyPart[], setBodyParts: (parts: ExtendedBodyPart[]) => void }) {
    const [, forceUpdate] = useReducer(x => x + 1, 0)

    const [bodyData, setBodyData] = useState<{ parts: Map<string, ExtendedBodyPart> }>({ parts: new Map() });

    useEffect(() => {
        const map = new Map<string, ExtendedBodyPart>()
        parts.forEach((part) => {
            map.set(part.slug?.toString() || "", part)
        })
        setBodyData({ parts: map })
    }, [])

    const onBodyPartPress = (slug?: Slug) => {
        if (!slug) return
        const temp = bodyData
        const oldSlug = temp.parts.get(slug)
        if (oldSlug) {
            if (oldSlug.intensity !== undefined) {
                let intensity = oldSlug.intensity + 1
                const newColor = getNewSlugColor(oldSlug.intensity)
                if (!newColor) {
                    temp.parts.delete(slug)
                } else {
                    temp.parts.set(slug, { ...oldSlug, intensity: intensity, color: newColor })
                }
            }
        } else {
            temp.parts.set(slug, { slug: slug, color: getNewSlugColor(0), intensity: 1 })
        }
        forceUpdate()
        setBodyParts([...bodyData.parts.values()])
    }

    return <div className=""> <Body
        data={[...bodyData.parts.values()]}
        side="front"
        gender="male"
        scale={1.}
        onBodyPartPress={(part) => onBodyPartPress(part.slug)}
    /></div>
}

export default EditExercise;