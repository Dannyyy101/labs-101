'use server'

import { auth } from "@/lib/auth"
import { backendFetch } from "@/utils/backend"
import { BACKEND_URL } from "@/utils/constants"
import { RunDetail, RunSummary } from "@/utils/types/run"
import { headers } from "next/headers"

async function userId() {
    const session = await auth.api.getSession({
        headers: await headers()
    })
    if (!session) throw new Error("User is currently not in a session")
    return session.user.id
}

export async function getRuns(from: Date, to: Date): Promise<RunSummary[]> {
    const url = new URL(`${BACKEND_URL}/users/${await userId()}/runs`)
    url.searchParams.append("from", from.toISOString())
    url.searchParams.append("to", to.toISOString())

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as RunSummary[]
    }

    throw new Error("Error fetching runs")
}

export async function getRun(id: string): Promise<RunDetail> {
    const url = new URL(`${BACKEND_URL}/users/${await userId()}/runs/${encodeURIComponent(id)}`)

    const response = await backendFetch(url.toString(), { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as RunDetail
    }

    throw new Error(`Error fetching run ${id}`)
}
