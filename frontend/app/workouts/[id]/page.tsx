'use client'
import { useParams } from "next/navigation";
import ExerciseModal from "../exercise";

export default function WorkoutPage() {
    const params = useParams<{ id: string }>()
    const workoutId = parseInt(params.id)

    const isCreating = workoutId === -1

    if (isCreating) {
        return <CreateWorkoutDialog />
    }
}

function CreateWorkoutDialog() {
    return <div className="w-full">
        <ExerciseModal />
    </div>
}