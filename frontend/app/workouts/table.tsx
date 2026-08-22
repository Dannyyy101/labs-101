'use client'


import {
    Table,
    TableBody,
    TableCaption,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { Workout } from "@/utils/types"

export function WorkoutsTable({ workouts }: { workouts: Workout[] }) {
    return <Table>
        <TableHeader>
            <TableRow className="relative">
                <TableHead className="w-[100px]">Name</TableHead>
                <TableHead className="w-[100px]">Duration</TableHead>
                <TableHead className="w-[100px]">Calories burned</TableHead>
                <TableHead className="w-[100px]">Avg. HeartRate</TableHead>
            </TableRow>
             
        </TableHeader>
        <TableBody>
            {workouts.map((workout) =>
                <TableRow key={workout.uuid}>
                    <TableCell className="font-medium">{workout.workoutActivityTypeName}</TableCell>
                    <TableCell className="font-medium">{workout.duration}</TableCell>
                    <TableCell className="font-medium">{workout.totalEnergyBurned.value + " " + workout.totalEnergyBurned.unit}</TableCell>
                    <TableCell className="font-medium">{workout.HKQuantityTypeIdentifierHeartRate.averageQuantity.value}</TableCell>
                </TableRow>
            )}
        </TableBody>
    </Table>
}