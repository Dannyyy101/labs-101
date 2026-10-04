import { getTrackedFood } from "./action";
import TrackFood from "./TrackFood";

const DAY = /^\d{4}-\d{2}-\d{2}$/

export default async function Page({ searchParams }: { searchParams: Promise<{ [key: string]: string | string[] | undefined }> }) {
    const { date } = await searchParams
    // the backend groups tracked food by UTC day, so ask for noon to always hit the selected day
    const day = typeof date === "string" && DAY.test(date) ? date : new Date().toISOString().slice(0, 10)
    const food = await getTrackedFood(new Date(`${day}T12:00:00Z`))
    return <TrackFood food={food} day={day} />
}
