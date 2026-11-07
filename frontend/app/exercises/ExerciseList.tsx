import { Dumbbell } from "lucide-react";
import { getAllExercises } from "./action";

import { Card } from "@/components/dashboard"
import { cn } from "@/lib/utils"
import { MUSCLES } from "../workouts/stats"

import { Exercise } from "@/utils/types/workoutTypes";
import {
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyMedia,
    EmptyTitle,
} from "@/components/ui/empty"
import EditExercise from "./EditExercise";

export default async function ExerciseList({ type }: { type: string }) {
    const exercises = await getAllExercises({ type });

    if (exercises.length === 0) {

        return <Empty className="border border-dashed">
            <EmptyHeader>
                <EmptyMedia variant="icon">
                    <Dumbbell />
                </EmptyMedia>
                <EmptyTitle>No exercises</EmptyTitle>
                <EmptyDescription>No exercises found</EmptyDescription>
            </EmptyHeader>
        </Empty>
    }

    return <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {exercises.map((exercise) => <ExerciseElement key={exercise.id} exercise={exercise} />)}
    </div>
}

function ExerciseElement({ exercise }: { exercise: Exercise }) {
    // primary muscles first, a muscle listed for both sides only once
    const muscles = [...new Map([...exercise.bodyParts]
        .sort((a, b) => (b.intensity ?? 0) - (a.intensity ?? 0))
        .filter((p) => p.slug)
        .map((p) => [p.slug!, p])).values()]

    return <Card className="flex flex-col p-4">
        <div className="flex items-start gap-2">
            <div className="min-w-0 flex-1">
                <h2 className="truncate text-heading" title={exercise.name}>{exercise.name}</h2>
                <p className="text-label font-medium text-ink-muted">{exercise.type}</p>
            </div>
            <EditExercise exercise={exercise}>Edit</EditExercise>
        </div>
        {exercise.description && <p className="mt-2 line-clamp-2 text-body text-ink-muted" title={exercise.description}>{exercise.description}</p>}
        {muscles.length > 0 && <div className="mt-auto flex flex-wrap gap-1 pt-3">
            {muscles.map((part) => <span key={part.slug} className={cn("rounded-full px-2 py-0.5 text-label font-medium",
                (part.intensity ?? 0) >= 3
                    ? "bg-brand-soft text-brand-strong"
                    : "glass-inset text-ink-muted")}>
                {MUSCLES[part.slug!] ?? part.slug}
            </span>)}
        </div>}
    </Card>
}
