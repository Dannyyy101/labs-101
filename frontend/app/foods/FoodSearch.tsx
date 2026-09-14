"use client"

import { useRouter, useSearchParams } from "next/navigation"
import { useState, useTransition } from "react"
import { Input } from "@/components/ui/input"

export function FoodSearch({ initialQuery }: { initialQuery: string }) {
    const [value, setValue] = useState(initialQuery)
    const [isPending, startTransition] = useTransition()
    const router = useRouter()
    const searchParams = useSearchParams()

    function submit(next: string) {
        const params = new URLSearchParams(searchParams)
        if (next.trim()) params.set("query", next.trim())
        else params.delete("query")
        params.set("page", "0")
        startTransition(() => router.push(`?${params}`))
    }

    return (
        <Input
            value={value}
            placeholder="Lebensmittel suchen…"
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && submit(value)}
            onBlur={() => submit(value)}
            className={`${isPending ? "opacity-70" : ""} max-w-96`}
        />
    )
}