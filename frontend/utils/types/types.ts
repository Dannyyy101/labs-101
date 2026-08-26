export enum WorkoutType {
    CYCLING = 13,
    RUNNING = 37,
    SWIMMING = 46,
    STRENGTH_TRAINING = 50,
    WALKING = 52
}

export interface Result<T> {
    value: T | null,
    error: Error | null
}

export interface WorkoutHeaderData {
    totalWorkouts: number,
    totalWorkoutTime: Item,
    caloriesBurned: Item
    restingHeartRateTimeline: Timeline[]
    VO2MaxTimeline: Timeline[]
    zone2Peace: Timeline[]
}

interface Timeline {
    value: number,
    date: Date
}

export interface Workout {
    uuid: string,
    workoutActivityType: WorkoutType,
    workoutActivityTypeName: string,
    startDate: Date,
    endDate: Date,
    duration: number,
    totalEnergyBurned: Item,
    HKQuantityTypeIdentifierHeartRate: HeartRate
}

interface Item {
    unit: string,
    value: number
}

interface HeartRate {
    sumQuantity: number | null,
    averageQuantity: Item,
    minimumQuantity: Item,
    maximumQuantity: Item,
    mostRecentQuantity: Item,
    mostRecentQuantityDateInterval: {
        start: Date,
        end: Date
    }
}



export interface Run extends Workout {
    totalDistance: {
        unit: string,
        value: number
    }
}