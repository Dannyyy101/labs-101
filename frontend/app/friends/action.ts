'use server'

import { backendFetch } from "@/utils/backend"
import { BACKEND_URL } from "@/utils/constants"
import { ApiError } from "@/utils/types/api"
import { FriendProfile, Friends } from "@/utils/types/friend"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

const FRIENDS = `${BACKEND_URL}/users/me/friends`

// the localized message of the backend if there is one ("Du hast dieses Training schon angefeuert")
async function request<T>(url: string, init: RequestInit, failure: string, parse: (response: Response) => Promise<T>): Promise<Result<T>> {
    try {
        const response = await backendFetch(url, { ...init, cache: 'no-store' })
        if (response.ok) {
            return { ok: true, data: await parse(response) }
        }
        console.error(`${failure}: ${response.status}`)
        const apiError = await response.json().catch(() => null) as ApiError | null
        return { ok: false, error: apiError?.errorMessage || failure }
    } catch (e) {
        console.error(failure, e)
        return { ok: false, error: `${failure}, der Server ist nicht erreichbar` }
    }
}

const json = <T,>(response: Response) => response.json() as Promise<T>
const none = async () => undefined

// the friends page and the open profile show the change right away
async function changed<T>(result: Result<T>, friendId?: string): Promise<Result<T>> {
    if (result.ok) {
        revalidatePath("/friends")
        if (friendId) revalidatePath(`/friends/${friendId}`)
    }
    return result
}

export async function getFriends(): Promise<Result<Friends>> {
    return request(FRIENDS, {}, "Freunde konnten nicht geladen werden", json<Friends>)
}

// null for someone who isn't a friend (the backend answers 404 then)
export async function getFriendProfile(friendId: string): Promise<Result<FriendProfile | null>> {
    const url = `${FRIENDS}/${encodeURIComponent(friendId)}/profile`
    try {
        const response = await backendFetch(url, { cache: 'no-store' })
        if (response.status === 404) return { ok: true, data: null }
        if (response.ok) return { ok: true, data: await response.json() as FriendProfile }
        console.error(`Profil konnte nicht geladen werden: ${response.status}`)
    } catch (e) {
        console.error("Profil konnte nicht geladen werden", e)
    }
    return { ok: false, error: "Profil konnte nicht geladen werden" }
}

export async function sendFriendRequest(userId: string): Promise<Result<void>> {
    return changed(await request(`${FRIENDS}/requests`, {
        method: "POST",
        body: JSON.stringify({ userId }),
        headers: { 'Content-Type': 'application/json' },
    }, "Anfrage konnte nicht gesendet werden", none))
}

export async function acceptFriendRequest(id: number): Promise<Result<void>> {
    return changed(await request(`${FRIENDS}/requests/${id}`, { method: "PUT" }, "Anfrage konnte nicht angenommen werden", none))
}

// declines a request to the user or takes back one of the user
export async function deleteFriendRequest(id: number): Promise<Result<void>> {
    return changed(await request(`${FRIENDS}/requests/${id}`, { method: "DELETE" }, "Anfrage konnte nicht entfernt werden", none))
}

export async function removeFriend(friendId: string): Promise<Result<void>> {
    return changed(await request(`${FRIENDS}/${encodeURIComponent(friendId)}`, { method: "DELETE" },
        "Freund konnte nicht entfernt werden", none), friendId)
}

export async function cheerFriend(friendId: string): Promise<Result<void>> {
    return changed(await request(`${FRIENDS}/${encodeURIComponent(friendId)}/cheers`, { method: "POST" },
        "Anfeuern hat nicht geklappt", none), friendId)
}
