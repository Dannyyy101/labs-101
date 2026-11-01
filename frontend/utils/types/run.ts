// running workouts from Apple Health, see RunController in the backend
// distances in meters, times in seconds, pace in seconds per kilometer

import type { Schemas } from "@/lib/api/types"

export type BestEffort = Schemas["BestEffortDto"]
export type RunSummary = Schemas["RunSummaryDto"]
export type RunPoint = Schemas["RunPointDto"]
export type RunSplit = Schemas["RunSplitDto"]
export type RunDetail = Schemas["RunDetailDto"]
