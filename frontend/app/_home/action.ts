'use server'

import { auth } from "@/lib/auth"
import { backendFetch } from "@/utils/backend"
import { BACKEND_URL } from "@/utils/constants"
import { HealthOverview } from "@/utils/types/overview"
import { headers } from "next/headers"

// `day` as "YYYY-MM-DD", today if missing
export async function getHealthOverview(day?: string): Promise<HealthOverview> {
    const session = await auth.api.getSession({
        headers: await headers()
    })
    if (!session) throw new Error("User is currently not in a session")

    const url = new URL(`${BACKEND_URL}/users/${session.user.id}/health/overview`)
    if (day) url.searchParams.append("date", day)

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as HealthOverview
    }

    throw new Error("Error fetching health overview")
}
