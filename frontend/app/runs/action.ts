'use server'

import { api } from "@/lib/api/client"
import { RunDetail, RunSummary } from "@/utils/types/run"

export async function getRuns(from: Date, to: Date): Promise<RunSummary[]> {
    const { data } = await api.GET("/api/users/me/runs", {
        params: { query: { from: from.toISOString(), to: to.toISOString() } },
    })

    if (!data) throw new Error("Error fetching runs")
    return data
}

export async function getRun(id: string): Promise<RunDetail> {
    const { data } = await api.GET("/api/users/me/runs/{id}", { params: { path: { id } } })

    if (!data) throw new Error(`Error fetching run ${id}`)
    return data
}
