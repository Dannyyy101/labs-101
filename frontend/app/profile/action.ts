'use server'

import { api } from "@/lib/api/client"
import { Profile } from "@/utils/types/profile"

export async function getProfile(): Promise<Profile> {
    const { data } = await api.GET("/api/users/me/profile")

    if (!data) throw new Error("Error fetching profile")
    return data
}
