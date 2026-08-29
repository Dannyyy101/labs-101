import { Dumbbell } from "lucide-react";
import { getAllExercises } from "./action";

import {
    Item,
    ItemActions,
    ItemContent,
    ItemDescription,
    ItemMedia,
    ItemTitle,
} from "@/components/ui/item"

import { Button } from "@/components/ui/button";
import { Exercise } from "@/utils/types/workoutTypes";
import {
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyMedia,
    EmptyTitle,
} from "@/components/ui/empty"
import EditExercise from "./EditExercise";
import { Badge } from "@/components/ui/badge";

export default async function ExerciseList({ }) {
    const exercises = await getAllExercises();

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

    return <div className="flex gap-2">{exercises.map((exercise) => <ExerciseElement key={exercise.id} exercise={exercise} />)}</div>
}

function ExerciseElement({ exercise }: { exercise: Exercise }) {
    return <Item variant={"outline"} className="min-w-20 max-w-96">
        <ItemContent>
            <ItemTitle>{exercise.name}</ItemTitle>
            <ItemDescription>
                {exercise.description}
                <Badge variant={"outline"} className="hover:cursor-pointer">{exercise.type}</Badge>
            </ItemDescription>
        </ItemContent>
        <ItemActions>
            <EditExercise exercise={exercise}>Edit</EditExercise>
        </ItemActions>
    </Item>
}
