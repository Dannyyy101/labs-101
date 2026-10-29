'use server'

import { api, toResult } from "@/lib/api/client"
import { FriendProfile, Friends } from "@/utils/types/friend"
import { Result } from "@/utils/types/result"
import { revalidatePath } from "next/cache"

// the friends page and the open profile show the change right away
async function changed<T>(result: Result<T>, friendId?: string): Promise<Result<T>> {
    if (result.ok) {
        revalidatePath("/friends")
        if (friendId) revalidatePath(`/friends/${friendId}`)
    }
    return result
}

export async function getFriends(): Promise<Result<Friends>> {
    return toResult(() => api.GET("/api/users/me/friends"), "Freunde konnten nicht geladen werden")
}

// null for someone who isn't a friend (the backend answers 404 then)
export async function getFriendProfile(friendId: string): Promise<Result<FriendProfile | null>> {
    try {
        const { data, response } = await api.GET("/api/users/me/friends/{friendId}/profile", { params: { path: { friendId } } })
        if (response.status === 404) return { ok: true, data: null }
        if (data) return { ok: true, data }
        console.error(`Profil konnte nicht geladen werden: ${response.status}`)
    } catch (e) {
        console.error("Profil konnte nicht geladen werden", e)
    }
    return { ok: false, error: "Profil konnte nicht geladen werden" }
}

export async function sendFriendRequest(userId: string): Promise<Result<void>> {
    return changed(await toResult(() => api.POST("/api/users/me/friends/requests", { body: { userId } }),
        "Anfrage konnte nicht gesendet werden"))
}

export async function acceptFriendRequest(id: number): Promise<Result<void>> {
    return changed(await toResult(() => api.PUT("/api/users/me/friends/requests/{id}", { params: { path: { id } } }),
        "Anfrage konnte nicht angenommen werden"))
}

// declines a request to the user or takes back one of the user
export async function deleteFriendRequest(id: number): Promise<Result<void>> {
    return changed(await toResult(() => api.DELETE("/api/users/me/friends/requests/{id}", { params: { path: { id } } }),
        "Anfrage konnte nicht entfernt werden"))
}

export async function removeFriend(friendId: string): Promise<Result<void>> {
    return changed(await toResult(() => api.DELETE("/api/users/me/friends/{friendId}", { params: { path: { friendId } } }),
        "Freund konnte nicht entfernt werden"), friendId)
}

export async function cheerFriend(friendId: string): Promise<Result<void>> {
    return changed(await toResult(() => api.POST("/api/users/me/friends/{friendId}/cheers", { params: { path: { friendId } } }),
        "Anfeuern hat nicht geklappt"), friendId)
}
