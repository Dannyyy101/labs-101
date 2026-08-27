'use client'
import { Exercise, WorkoutTemplate } from "@/utils/types/workoutTypes";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "../ui/accordion";
import { Card, CardDescription, CardHeader, CardTitle } from "../ui/card";
import { Dispatch, SetStateAction, useEffect, useState } from "react";
import { getAllExercises } from "./action";
import { Spinner } from "../ui/spinner";

export function ExerciseSelect({ training, selectedExerciseIds, setSelectedExerciseIds }: {
    training: WorkoutTemplate, selectedExerciseIds: { ids: Map<string, number>, len: number }, setSelectedExerciseIds: Dispatch<SetStateAction<{
        ids: Map<string, number>;
        len: number;
    }>>
}) {
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<string>("")
    const [exercises, setExercises] = useState<Exercise[]>([])


    useEffect(() => {
        const fetch = async () => {
            try {
                setExercises(await getAllExercises())
            } catch (error: any) {
                setError(error.message)
            } finally {
                setLoading(false)
            }
        }
        fetch()
    }, [])

    return <Accordion>
        <AccordionItem key={training.id} value={training.name}>
            <AccordionTrigger>{training.name}</AccordionTrigger>
            <AccordionContent className="flex gap-2 flex-wrap">
                {!loading ?
                    <>
                        {
                            exercises.map((exercise) =>
                                <Card
                                    key={exercise.id}
                                    style={{ borderWidth: !!selectedExerciseIds.ids.get(exercise.id) ? "1px" : "0" }}
                                    className="w-md border-black relative"
                                    onClick={() => {
                                        setSelectedExerciseIds((prev) => {
                                            const next = new Map(prev.ids);
                                            next.set(exercise.id, prev.ids.get(exercise.id) ? 0 : prev.len + 1);
                                            const len = prev.ids.get(exercise.id) ? prev.len - 1 : prev.len + 1
                                            return { ids: next, len: len };
                                        });
                                    }}
                                >
                                    <CardHeader>
                                        <p style={{ borderWidth: `${selectedExerciseIds.ids.get(exercise.id) ? "1px" : ""}` }} className="absolute top-2 right-2 p-3 w-4 h-4 flex justify-center items-center rounded-full border-0">{selectedExerciseIds.ids.get(exercise.id) ? selectedExerciseIds.ids.get(exercise.id) : ""}</p>
                                        <CardTitle>{exercise.name}</CardTitle>
                                        <CardDescription>{exercise.description}</CardDescription>
                                    </CardHeader>
                                </Card>
                            )
                        }</> : <ErrorOrLoadingScreen error={error} loading={loading} />
                }
            </AccordionContent>
        </AccordionItem>
    </Accordion>
}

const ErrorOrLoadingScreen = ({ error, loading }: { error: string, loading: boolean }) => {
    if (loading)
        return <Spinner className="size-6" />

    return <div><p>{error}</p></div>
}