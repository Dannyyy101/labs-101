import { getAllExercises } from "../exercises/action";
import { getActiveSession, getAllWorkouts, getSessions } from "./action";
import WorkoutDashboard from "./WorkoutDashboard";

export default async function Page() {
    // this year for the records, at least two 30 day periods for the comparison with the period before
    const now = new Date()
    const from = new Date(Math.min(new Date(now.getFullYear(), 0, 1).getTime(), now.getTime() - 62 * 864e5))
    const [workouts, sessions, active, exercises] = await Promise.all([
        getAllWorkouts(),
        getSessions(from, new Date(now.getTime() + 864e5)),
        getActiveSession(),
        getAllExercises(),
    ])
    return <WorkoutDashboard workouts={workouts} sessions={sessions} active={active} exercises={exercises} />
}
