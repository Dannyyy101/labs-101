'use server'

import { backendFetch } from "@/utils/backend"
import { BACKEND_URL } from "@/utils/constants"
import { Profile } from "@/utils/types/profile"

export async function getProfile(): Promise<Profile> {
    const response = await backendFetch(`${BACKEND_URL}/users/me/profile`, { cache: 'no-store' })

    if (response.ok) {
        return await response.json() as Profile
    }

    throw new Error("Error fetching profile")
}
