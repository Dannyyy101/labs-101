import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs"
import ExerciseList from "./ExerciseList"
import EditExercise from "./EditExercise"

export default function Exercises() {
    return <div className="w-full h-screen flex justify-center">
        <div className="w-10/12 mt-10">
            <h1 className="text-2xl font-bold">Exercises</h1>

            <Tabs defaultValue="overview" className="w-full mt-4 relative">
                <EditExercise className="w-32 bg-accent top-0 right-4 absolute" exercise={{ name: "", description: "", id: -1, type: "", bodyParts: [] }}>Create exercise</EditExercise>
                <TabsList>
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