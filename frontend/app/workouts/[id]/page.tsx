import { getAllExercises } from "@/app/exercises/action";
import { getWorkoutById } from "../action";
import WorkoutEditor from "./WorkoutEditor";
import { notFound } from "next/navigation";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params
    // "-1" was used for new workouts before
    const isNew = id === "new" || id === "-1"
    const workoutId = Number(id)
    if (!isNew && !Number.isInteger(workoutId)) notFound()

    const [workout, exercises] = await Promise.all([
        isNew ? null : getWorkoutById(workoutId),
        getAllExercises(),
    ])
    return <WorkoutEditor workout={workout} exercises={exercises} />
}
