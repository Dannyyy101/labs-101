'use server'

import { backendFetch } from "@/utils/backend"
import { BACKEND_URL } from "@/utils/constants"
import { RunDetail, RunSummary } from "@/utils/types/run"

export async function getRuns(from: Date, to: Date): Promise<RunSummary[]> {
    const url = new URL(`${BACKEND_URL}/users/me/runs`)
    url.searchParams.append("from", from.toISOString())
    url.searchParams.append("to", to.toISOString())

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as RunSummary[]
    }

    throw new Error("Error fetching runs")
}

export async function getRun(id: string): Promise<RunDetail> {
    const url = new URL(`${BACKEND_URL}/users/me/runs/${encodeURIComponent(id)}`)

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as RunDetail
    }

    throw new Error(`Error fetching run ${id}`)
}
