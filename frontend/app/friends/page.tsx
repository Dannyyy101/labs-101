import ErrorState from "@/components/ErrorState"
import { getFriends } from "./action"
import FriendsView from "./FriendsView"

export default async function Page() {
    const friends = await getFriends()

    if (!friends.ok)
        return <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
            <div className="mx-auto max-w-2xl">
                <ErrorState title="Freunde nicht verfügbar" message={friends.error} />
            </div>
        </div>

    return <FriendsView friends={friends.data} />
}
