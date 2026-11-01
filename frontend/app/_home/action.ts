'use server'

import { api } from "@/lib/api/client"
import { HealthOverview } from "@/utils/types/overview"

// `day` as "YYYY-MM-DD", today if missing
export async function getHealthOverview(day?: string): Promise<HealthOverview> {
    const { data } = await api.GET("/api/users/me/health/overview", { params: { query: { date: day } } })

    if (!data) throw new Error("Error fetching health overview")
    return data
}
