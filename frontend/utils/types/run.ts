// running workouts from Apple Health, see RunController in the backend
// distances in meters, times in seconds, pace in seconds per kilometer

export interface BestEffort {
    name: string
    distance: number
    time: number
}

export interface RunSummary {
    id: string
    name: string
    startDate: string
    endDate: string
    timeZone: string
    indoor: boolean
    sourceName: string | null
    distance: number
    // moving time without pauses
    duration: number
    elevationGain: number | null
    averageHeartRate: number | null
    maxHeartRate: number | null
    energy: number | null
    cadence: number | null
    // seconds in Z1–Z5
    zoneSeconds: number[]
    // [latitude, longitude]
    thumbnail: [number, number][]
    bestEfforts: BestEffort[]
}

export interface RunPoint {
    distance: number
    time: number
    latitude: number
    longitude: number
    elevation: number | null
    pace: number | null
    heartRate: number | null
}

export interface RunSplit {
    index: number
    distance: number
    time: number
    heartRate: number | null
}

export interface RunDetail {
    summary: RunSummary
    points: RunPoint[]
    splits: RunSplit[]
}
