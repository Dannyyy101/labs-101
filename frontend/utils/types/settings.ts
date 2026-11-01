import type { Schemas } from "@/lib/api/types"

export type UserSettings = Schemas["UserSettingsDto"]

// used as long as the user hasn't saved a goal yet
export const DEFAULT_CALORIE_GOAL = 3000
