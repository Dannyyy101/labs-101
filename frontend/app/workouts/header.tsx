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
import { WorkoutHeaderData } from "@/utils/types"

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


export function Header({ workoutHeaderData }: { workoutHeaderData: WorkoutHeaderData }) {
    return (
        <Tabs defaultValue="overview" >
            <TabsList>
                <TabsTrigger value="overview">Overview</TabsTrigger>
                <TabsTrigger value="running">Running</TabsTrigger>
                <TabsTrigger value="cycling">Cycling</TabsTrigger>
                <TabsTrigger value="swimming">Swimming</TabsTrigger>
                <TabsTrigger value="strength-training">Strength Training</TabsTrigger>
            </TabsList>
            <TabsContent value="overview">
                <section className="flex w-full gap-6 justify-between">
                    <BadgeModal badge={{ value: workoutHeaderData.totalWorkouts, description: "Total workouts", icon: <Dumbbell/> }} />
                    <BadgeModal badge={{ value: workoutHeaderData.totalWorkoutTime.value / 60 + "h", description: "Total workout time", icon: <Clock/> }} />
                    <BadgeModal badge={{ value: workoutHeaderData.caloriesBurned.value + " " + workoutHeaderData.caloriesBurned.unit, description: "Total calories burned", icon: <Flame/> }} />
                </section>
            </TabsContent>
        </Tabs>
    )
}
