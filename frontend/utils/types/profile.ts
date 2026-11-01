// the profile page: login streak, activity heatmap and achievements, see UserProfileController in the backend
// days as "YYYY-MM-DD", distances in meters

import type { Schemas } from "@/lib/api/types"

export type Streak = Schemas["StreakDto"]
export type ActivityDay = Schemas["ActivityDayDto"]
export type Achievement = Schemas["AchievementDto"]
export type Profile = Schemas["ProfileDto"]
