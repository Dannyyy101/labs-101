import { getAllExercises } from "@/app/exercises/action";
import { redirect } from "next/navigation";
import { getActiveSession, getSessions } from "../action";
import SessionView from "./SessionView";

export default async function Page() {
    const active = await getActiveSession()
    if (!active) redirect("/workouts")

    const now = new Date()
    const [exercises, history] = await Promise.all([
        getAllExercises(),
        // for the "last time" hints of the sets
        getSessions(new Date(now.getTime() - 365 * 864e5), now),
    ])
    return <SessionView session={active} exercises={exercises} history={history} />
}
