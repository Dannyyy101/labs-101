import { getRun, getRuns } from "./action";
import RunDashboard from "./RunDashboard";

export default async function Page() {
    // this year for the records, at least two 30 day periods for the comparison with the period before
    const now = new Date()
    const from = new Date(Math.min(new Date(now.getFullYear(), 0, 1).getTime(), now.getTime() - 62 * 864e5))
    const runs = await getRuns(from, new Date(now.getTime() + 864e5))
    const detail = runs.length ? await getRun(runs[0].id) : null
    return <RunDashboard runs={runs} initialDetail={detail} />
}
