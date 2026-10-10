export interface UserSettings {
    userId: string
    calorieGoal: number | null
}

// used as long as the user hasn't saved a goal yet
export const DEFAULT_CALORIE_GOAL = 3000
