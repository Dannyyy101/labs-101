import { WorkoutType } from "@/utils/types/types";

export function workoutTypeToName(workoutType: WorkoutType): string {
    switch (workoutType) {
        case WorkoutType.CYCLING:
            return "Cycling"
        case WorkoutType.RUNNING:
            return "Run"
        case WorkoutType.SWIMMING:
            return "Swimming"
        case WorkoutType.STRENGTH_TRAINING:
            return "Strength Training"
        case WorkoutType.WALKING:
            return "Walking"
        default:
            break;
    }
    console.log("Invalid workout type")
    return "-"
}