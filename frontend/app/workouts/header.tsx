import { Clock, Flame, Dumbbell } from "lucide-react"
import {
    Item,
    ItemContent,
    ItemDescription,
    ItemMedia,
    ItemTitle,
} from "@/components/ui/item"
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs"
import { WorkoutHeaderData, WorkoutType } from "@/utils/types"


import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Progress } from "./progress"
import { useWorkoutData } from "@/hooks/useWorkoutData"

const items = [
    { label: "1 Month", value: null },
    { label: "1 Week", value: "1-week" },
    { label: "3 Months", value: "3-months" },
    { label: "6 Months", value: "6-months" },
    { label: "1 Year", value: "1-year" },
    { label: "All Time", value: "all-time" },
]
export function TimeSpanSelect() {
    return (
        <Select items={items}>
            <SelectTrigger className="w-full max-w-48">
                <SelectValue />
            </SelectTrigger>
            <SelectContent>
                <SelectGroup>
                    <SelectLabel>Timespan</SelectLabel>
                    {items.map((item) => (
                        <SelectItem key={item.value} value={item.value}>
                            {item.label}
                        </SelectItem>
                    ))}
                </SelectGroup>
            </SelectContent>
        </Select>
    )
}


interface Badge {
    value: string | number,
    description: string,
    icon: any
}

function BadgeModal({ badge }: { badge: Badge }) {
    return <Item variant={"outline"} className="w-96">
        <ItemMedia variant="icon">
            {badge.icon}
        </ItemMedia>
        <ItemContent>
            <ItemTitle>{badge.value}</ItemTitle>
            <ItemDescription>
                {badge.description}
            </ItemDescription>
        </ItemContent>
    </Item>
}




export function Header({ workoutHeaderData, setWorkoutType }: { workoutHeaderData: WorkoutHeaderData, setWorkoutType: (type: WorkoutType | null) => void }) {

    return (
        <Tabs defaultValue="overview" >
            <TabsList>
                <TabsTrigger onClick={() => setWorkoutType(null)} value="overview">Overview</TabsTrigger>
                <TabsTrigger onClick={() => setWorkoutType(WorkoutType.RUNNING)} value="running">Running</TabsTrigger>
                <TabsTrigger onClick={() => setWorkoutType(WorkoutType.CYCLING)} value="cycling">Cycling</TabsTrigger>
                <TabsTrigger onClick={() => setWorkoutType(WorkoutType.SWIMMING)} value="swimming">Swimming</TabsTrigger>
                <TabsTrigger onClick={() => setWorkoutType(WorkoutType.STRENGTH_TRAINING)} value="strength-training">Strength Training</TabsTrigger>
                <div className="absolute right-10"><TimeSpanSelect /></div>

            </TabsList>
            <TabsContent value="overview">
                <section className="flex w-full gap-6 justify-between">
                    <BadgeModal badge={{ value: workoutHeaderData.totalWorkouts, description: "Total workouts", icon: <Dumbbell /> }} />
                    <BadgeModal badge={{ value: workoutHeaderData.totalWorkoutTime.value / 60 + "h", description: "Total workout time", icon: <Clock /> }} />
                    <BadgeModal badge={{ value: workoutHeaderData.caloriesBurned.value + " " + workoutHeaderData.caloriesBurned.unit, description: "Total calories burned", icon: <Flame /> }} />
                </section>

                <Separator className="my-8"/>

                <div className="flex justify-between gap-4">
                    <>
                        <Progress title="Resting heart rate" description="" chartData={workoutHeaderData.restingHeartRateTimeline} />
                        <Progress title="VO2Max" description="" chartData={workoutHeaderData.VO2MaxTimeline} />
                        <Progress title="Zone 2 peace" description="" chartData={workoutHeaderData.zone2Peace} />
                    </>
                </div>
            </TabsContent>
            <TabsContent value="running">
                <section className="flex w-full gap-6 justify-between">
                    <BadgeModal badge={{ value: workoutHeaderData.totalWorkouts, description: "Total workouts", icon: <Dumbbell /> }} />
                    <BadgeModal badge={{ value: workoutHeaderData.totalWorkoutTime.value / 60 + "h", description: "Total workout time", icon: <Clock /> }} />
                    <BadgeModal badge={{ value: workoutHeaderData.caloriesBurned.value + " " + workoutHeaderData.caloriesBurned.unit, description: "Total calories burned", icon: <Flame /> }} />
                </section>

                <Separator className="my-8"/>

                <div className="flex justify-between gap-4">
                    <>
                        <Progress title="Resting heart rate" description="" chartData={workoutHeaderData.restingHeartRateTimeline} />
                        <Progress title="VO2Max" description="" chartData={workoutHeaderData.VO2MaxTimeline} />
                        <Progress title="Zone 2 peace" description="" chartData={workoutHeaderData.zone2Peace} />
                    </>
                </div>
            </TabsContent>
        </Tabs>
    )
}
