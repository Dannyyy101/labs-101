import ErrorState from "@/components/ErrorState"
import ProfileView from "@/app/profile/ProfileView"
import { notFound } from "next/navigation"
import { getFriendProfile } from "../action"
import { FriendHeader } from "./FriendHeader"

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params
    const result = await getFriendProfile(decodeURIComponent(id))

    if (!result.ok)
        return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
            <div className="mx-auto max-w-2xl">
                <ErrorState title="Profil nicht verfügbar" message={result.error} />
            </div>
        </div>
    // not (or no longer) a friend
    if (!result.data) notFound()

    const { friend, profile } = result.data
    return <ProfileView profile={profile} header={<FriendHeader friend={friend} memberSince={profile.memberSince} />} />
}
