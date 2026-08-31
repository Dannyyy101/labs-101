'use client'
import { Separator } from "@/components/ui/separator"
import { Header } from "./header";
import { WorkoutsTable } from "./table";
import { useWorkoutData } from "@/hooks/useWorkoutData";
import { WorkoutType } from "@/utils/types/types";
import { WorkoutMap } from "./workoutMap";

export default function WorkoutsPage() {
    const { workoutHeaderData, workouts, workoutType, setWorkoutType } = useWorkoutData();


    if (!workoutHeaderData || !workouts) {
        return <></>
    }


    const showLastTrainingInformation = () => {
        switch (workoutType) {
            case WorkoutType.RUNNING:
                return <WorkoutMap/>
            case WorkoutType.CYCLING:
                return <WorkoutMap/>
            case null:
                return <WorkoutsTable workouts={workouts} />
            default:
                return <></>;
        }
    }


    return <main className="w-screen h-screen p-8 flex flex-col gap-8">
        <div>
            {workoutHeaderData && <Header workoutHeaderData={workoutHeaderData} setWorkoutType={setWorkoutType} />}
        </div>

        <Separator />

        <div>
            {showLastTrainingInformation()}
        </div>
    </main>
}

