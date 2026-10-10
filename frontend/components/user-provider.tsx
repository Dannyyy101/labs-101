'use client'

import type { User } from "@/lib/auth"
import { createContext, ReactNode, useContext } from "react"

const UserContext = createContext<User | null>(null)

/** The signed in user for client components, the layout reads it from the session cookie. */
export function UserProvider({ user, children }: { user: User | null, children: ReactNode }) {
    return <UserContext.Provider value={user}>{children}</UserContext.Provider>
}

export function useUser(): User | null {
    return useContext(UserContext)
}
