import { getTrackedFood } from "./action";
import TrackFood from "./TrackFood";
import { getSettings } from "@/app/settings/action";
import { DEFAULT_CALORIE_GOAL } from "@/utils/types/settings";

const DAY = /^\d{4}-\d{2}-\d{2}$/

export default async function Page({ searchParams }: { searchParams: Promise<{ [key: string]: string | string[] | undefined }> }) {
    const { date } = await searchParams
    // the backend groups tracked food by UTC day, so ask for noon to always hit the selected day
    const day = typeof date === "string" && DAY.test(date) ? date : new Date().toISOString().slice(0, 10)
    const [food, settings] = await Promise.all([getTrackedFood(new Date(`${day}T12:00:00Z`)), getSettings()])
    // without settings the default goal is good enough to keep tracking
    const calorieGoal = settings.ok ? settings.data?.calorieGoal ?? DEFAULT_CALORIE_GOAL : DEFAULT_CALORIE_GOAL
    return <TrackFood food={food} day={day} calorieGoal={calorieGoal} settingsError={settings.ok ? undefined : settings.error} />
}
