// the home page: sleep, recovery and strain of a day, see HealthOverviewController in the backend
// times in seconds, energy in kcal, distances in meters, scores 0–100

import type { Schemas } from "@/lib/api/types"

export type SleepStage = "AWAKE" | "CORE" | "DEEP" | "REM" | "ASLEEP"

export type Sleep = Schemas["SleepDto"]
export type Recovery = Schemas["RecoveryDto"]
export type Strain = Schemas["StrainDto"]
export type Activity = Schemas["ActivityDto"]
export type OverviewWorkout = Schemas["OverviewWorkoutDto"]
export type DayScore = Schemas["DayScoreDto"]
export type HealthOverview = Schemas["HealthOverviewDto"]
