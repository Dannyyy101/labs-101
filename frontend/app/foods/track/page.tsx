import { getTrackedFood } from "./action";
import TrackFood from "./TrackFood";

export default async function Page() {
    const food = await getTrackedFood(new Date())
    return <TrackFood food={food} />
}