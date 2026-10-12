// the profile page: login streak, activity heatmap and achievements, see UserProfileController in the backend
// days as "YYYY-MM-DD", distances in meters

export interface Streak {
    // still counts while today is missing, it breaks after a whole day without the app
    current: number
    record: number
    today: boolean
}

export interface ActivityDay {
    date: string
    // breakfast, lunch, … with at least one tracked food
    meals: number
    runs: number
    runDistance: number
    workouts: number
    workoutMinutes: number
    loggedIn: boolean
}

export interface Achievement {
    id: string
    // null while there is nothing to compare, for SUB_25 the best 5 km time in seconds (lower is better)
    progress: number | null
    target: number
    // day it was first reached, null while open
    achievedAt: string | null
}

export interface Profile {
    memberSince: string | null
    today: string
    streak: Streak
    // days of the last 53 weeks with any activity, oldest first
    days: ActivityDay[]
    achievements: Achievement[]
    calorieGoal: number | null
}
