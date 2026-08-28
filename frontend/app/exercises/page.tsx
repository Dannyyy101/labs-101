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
import { Button } from "@/components/ui/button"
import { TrashIcon } from "lucide-react"

export default function Exercises() {
    return <div className="w-screen h-screen flex justify-center">
        <div className="w-10/12 mt-10">
            <h1 className="text-2xl font-bold">Exercises</h1>

            <Tabs defaultValue="overview" className="w-full mt-4 relative">
                <EditExercise className="w-32 bg-accent top-0 right-4 absolute" exercise={{ name: "", description: "", id: -1, type: "" }}>Create exercise</EditExercise>
                <TabsList>
                    <TabsTrigger value="overview">Overview</TabsTrigger>
                    <TabsTrigger value="strength-training">Strength Training</TabsTrigger>
                    <TabsTrigger value="running">Running</TabsTrigger>
                    <TabsTrigger value="swimming">Swimming</TabsTrigger>
                    <TabsTrigger value="stretching">Stretching</TabsTrigger>
                </TabsList>
                <TabsContent value="overview">
                    <ExerciseList />
                </TabsContent>
                <TabsContent value="analytics">

                </TabsContent>
                <TabsContent value="reports">
                    <Card>
                        <CardHeader>
                            <CardTitle>Reports</CardTitle>
                            <CardDescription>
                                Generate and download your detailed reports. Export data in
                                multiple formats for analysis.
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="text-sm text-muted-foreground">
                            You have 5 reports ready and available to export.
                        </CardContent>
                    </Card>
                </TabsContent>
                <TabsContent value="settings">
                    <Card>
                        <CardHeader>
                            <CardTitle>Settings</CardTitle>
                            <CardDescription>
                                Manage your account preferences and options. Customize your
                                experience to fit your needs.
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="text-sm text-muted-foreground">
                            Configure notifications, security, and themes.
                        </CardContent>
                    </Card>
                </TabsContent>
            </Tabs>
        </div>
    </div>
}