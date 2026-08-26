'use client'

import { getAllWorkouts, getWorkoutsHeaderData } from "@/app/workouts/action";
import { Workout, WorkoutHeaderData, WorkoutType } from "@/utils/types/types";
import { useEffect, useState } from "react"
import { Router } from "next/router";

export function useWorkoutData() {
    const [workoutType, setWorkoutType] = useState<WorkoutType | null>(null);
    const [workoutHeaderData, setWorkoutHeaderData] = useState<WorkoutHeaderData | null>(null);
    const [workouts, setWorkouts] = useState<Workout[] | null>(null);


    useEffect(() => {
        const fetch = async () => {
            setWorkoutHeaderData((await getWorkoutsHeaderData()).value);
            setWorkouts((await getAllWorkouts()).value);
        }
        fetch()
        
    }, [workoutType])


    return {workoutType, workoutHeaderData, workouts, setWorkoutType}
}