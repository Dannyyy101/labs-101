import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs"
import ExerciseList from "./ExerciseList"
import EditExercise from "./EditExercise"

export default function Exercises() {
    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-7xl flex-col gap-5">
            <div className="flex flex-wrap items-end gap-4">
                <h1 className="min-w-56 flex-1 text-[34px] font-bold leading-tight tracking-tight">Exercises</h1>
                <EditExercise exercise={{ name: "", description: "", id: -1, type: "Strength Training", bodyParts: [] }}>Create exercise</EditExercise>
            </div>

            <Tabs defaultValue="overview" className="w-full">
                <TabsList className="max-w-full overflow-x-auto">
                    <TabsTrigger value="overview">Overview</TabsTrigger>
                    <TabsTrigger value="strength-training">Strength Training</TabsTrigger>
                    <TabsTrigger value="running">Running</TabsTrigger>
                    <TabsTrigger value="swimming">Swimming</TabsTrigger>
                    <TabsTrigger value="stretching">Stretching</TabsTrigger>
                </TabsList>
                <TabsContent value="overview">
                    <ExerciseList type="" />
                </TabsContent>
                <TabsContent value="strength-training">
                    <ExerciseList type="Strength Training" />
                </TabsContent>
                <TabsContent value="running">
                    <ExerciseList type="Running" />
                </TabsContent>
                <TabsContent value="swimming">
                    <ExerciseList type="Swimming" />
                </TabsContent>
                <TabsContent value="stretching">
                    <ExerciseList type="Stretching" />
                </TabsContent>
            </Tabs>
        </div>
    </div>
}