import { Separator } from "@/components/ui/separator"
import { getWorkoutsHeaderData, getAllWorkouts } from "./action";
import { Header } from "./header";
import { WorkoutsTable } from "./table";
import { Progress } from "./progress";

export default async function WorkoutsPage() {
    const workoutsHeaderData = await getWorkoutsHeaderData();
    const workouts = await getAllWorkouts();
    return <main className="w-screen h-screen p-8 flex flex-col gap-8">
        <div>
            {workoutsHeaderData.value && <Header workoutHeaderData={workoutsHeaderData.value} />}
        </div>
        <Separator />

        <div className="flex justify-between gap-4">
            {workoutsHeaderData.value && <>
                <Progress title="Resting heart rate" description="" chartData={workoutsHeaderData.value.restingHeartRateTimeline} />
                <Progress title="VO2Max" description="" chartData={workoutsHeaderData.value.VO2MaxTimeline}/>
                <Progress title="Zone 2 peace" description="" chartData={workoutsHeaderData.value.zone2Peace}/>

            </>
            }
        </div>
        <Separator />
        <div>
            {workouts.value &&
                <WorkoutsTable workouts={workouts.value} />
            }
        </div>
    </main>
}

