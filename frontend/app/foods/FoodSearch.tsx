"use client"

import { useRouter, useSearchParams } from "next/navigation"
import { useState, useTransition } from "react"
import { cn } from "@/lib/utils"
import { Plus, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import EditFood, { EMPTY_FOOD } from "./EditFood"
import { FoodWithPortion } from "@/utils/types/food"

export function FoodSearch({ initialQuery }: { initialQuery: string }) {
    const [value, setValue] = useState(initialQuery)
    const [isPending, startTransition] = useTransition()
    const router = useRouter()
    const searchParams = useSearchParams()
    const [showCreateFood, setShowCreateFood] = useState<FoodWithPortion>();

    function submit(next: string) {
        const params = new URLSearchParams(searchParams)
        if (next.trim()) params.set("query", next.trim())
        else params.delete("query")
        params.set("page", "0")
        startTransition(() => router.push(`?${params}`))
    }

    return (
        <div className="flex w-full gap-2 sm:w-auto">
            <label className="glass flex h-9 min-w-0 flex-1 items-center gap-2 rounded-[10px] px-3 text-ink-muted focus-within:ring-3 focus-within:ring-brand/30 sm:w-80 sm:flex-none">
                <Search className="size-4 shrink-0" />
                <input
                    value={value}
                    placeholder="Lebensmittel suchen"
                    aria-label="Lebensmittel suchen"
                    onChange={(e) => setValue(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && submit(value)}
                    onBlur={() => submit(value)}
                    className={cn("min-w-0 flex-1 bg-transparent text-ink outline-none placeholder:text-ink-muted", isPending && "opacity-70")}
                />
            </label>
            <Button className="shrink-0" onClick={() => setShowCreateFood(EMPTY_FOOD)}><Plus />Lebensmittel anlegen</Button>
            <EditFood food={showCreateFood} close={() => setShowCreateFood(undefined)} />
        </div>
    )
}