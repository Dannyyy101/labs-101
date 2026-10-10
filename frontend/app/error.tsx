'use client'

import ErrorState from "@/components/ErrorState"
import { useEffect } from "react"

// last line of defense for every page, the navbar of the root layout stays usable
export default function Error({ error, retry }: { error: Error & { digest?: string }, retry: () => void }) {
    useEffect(() => {
        console.error(error)
    }, [error])

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto max-w-2xl">
            <ErrorState
                message="Die Seite konnte nicht geladen werden. Bitte versuche es gleich noch einmal."
                retry={retry}
            />
        </div>
    </div>
}
