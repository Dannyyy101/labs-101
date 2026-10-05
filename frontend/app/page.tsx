import { getHealthOverview } from "./_home/action";
import HealthHome from "./_home/HealthHome";

const DAY = /^\d{4}-\d{2}-\d{2}$/

export default async function Home({ searchParams }: { searchParams: Promise<{ [key: string]: string | string[] | undefined }> }) {
  const { date } = await searchParams
  const overview = await getHealthOverview(typeof date === "string" && DAY.test(date) ? date : undefined)
  return <HealthHome overview={overview} />
}
