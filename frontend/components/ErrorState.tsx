'use client'

import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/ui/spinner"
import { AlertCircle, RotateCw } from "lucide-react"
import { useRouter } from "next/navigation"
import { useTransition } from "react"

// shown instead of content that couldn't be loaded, "retry" loads the page again
export default function ErrorState({ title = "Etwas ist schiefgelaufen", message, retry }: {
    title?: string
    message: string
    retry?: () => void
}) {
    const router = useRouter()
    const [retrying, startRetrying] = useTransition()

    return <div role="alert" className="rounded-3xl bg-card shadow-sm p-6 flex flex-col items-center gap-3 text-center">
        <AlertCircle className="size-8 text-destructive" />
        <div>
            <h2 className="text-lg font-semibold">{title}</h2>
            <p className="text-muted-foreground mt-1">{message}</p>
        </div>
        <Button variant="outline" disabled={retrying} onClick={() => startRetrying(() => retry ? retry() : router.refresh())}>
            {retrying ? <Spinner /> : <RotateCw />}Erneut versuchen
        </Button>
    </div>
}
