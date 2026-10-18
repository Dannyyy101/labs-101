// friends and friend requests, see FriendController in the backend

import { CalendarUser } from "./calendarTypes"
import { Profile } from "./profile"

export interface Training {
    sessionId: number
    // of the template it was started from, null for a free workout
    name: string | null
    startedAt: string
    // already cheered on, it works once per workout
    cheered: boolean
}

export interface Friend {
    id: string
    name: string | null
    image: string | null
    // the request was accepted
    since: string
    // null while not training
    training: Training | null
}

export interface FriendRequest {
    id: number
    // who sent it (incoming) or who it was sent to (outgoing)
    user: CalendarUser
    createdAt: string
}

export interface Friends {
    // the ones training first, then by name
    friends: Friend[]
    incoming: FriendRequest[]
    outgoing: FriendRequest[]
}

export interface FriendProfile {
    friend: Friend
    profile: Profile
}
