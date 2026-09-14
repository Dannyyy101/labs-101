import { Empty, EmptyHeader, EmptyTitle, EmptyDescription, EmptyContent } from "@/components/ui/empty";
import { getAllWorkouts } from "./action";
import Link from "next/link";
import { Item, ItemContent, ItemTitle } from "@/components/ui/item";


export default async function Workouts() {
    const workouts = await getAllWorkouts()

    if (workouts.length === 0) {
        return <EmptyExercises />
    }

    return <div className="flex">{workouts.map((workout) =>
        <Item key={workout.id}>
            <Link href={`/workouts/${workout.id}`}>
                <ItemContent>
                    <ItemTitle>{workout.name}</ItemTitle>
                </ItemContent>
            </Link>
        </Item>
    )}</div>
}

function EmptyExercises() {

    return (
        <Empty className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
            <EmptyHeader>
                <EmptyTitle>No Workouts Yet</EmptyTitle>
                <EmptyDescription>
                    You haven&apos;t created any workouts yet. Get started by creating
                    your first workout.
                </EmptyDescription>
            </EmptyHeader>
            <EmptyContent className="flex-row justify-center gap-2">
                <Link href={"/workouts/-1"}>Create Workout</Link>
            </EmptyContent>
        </Empty>
    )
}