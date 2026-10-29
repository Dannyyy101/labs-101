import { getAllExercises } from "@/app/exercises/action";
import { getFriends } from "@/app/friends/action";
import { getWorkoutById } from "../action";
import WorkoutEditor from "./WorkoutEditor";
import { notFound } from "next/navigation";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params
    // "-1" was used for new workouts before
    const isNew = id === "new" || id === "-1"
    const workoutId = Number(id)
    if (!isNew && !Number.isInteger(workoutId)) notFound()

    const [workout, exercises, friends] = await Promise.all([
        isNew ? null : getWorkoutById(workoutId),
        getAllExercises(),
        // to share the template with, without them it just can't be shared
        isNew ? null : getFriends(),
    ])
    return <WorkoutEditor workout={workout} exercises={exercises} friends={friends?.ok ? friends.data.friends : []} />
}
