'use client'

import { cn } from "@/lib/utils"

const MAX_REPS = 50

/** Reps picked from a native select, phones show it as a wheel instead of a tiny number field. */
export default function RepsPicker({ value, onChange, label, className }: { value: number, onChange: (reps: number) => void, label: string, className?: string }) {
    const max = Math.max(MAX_REPS, value)
    return <select aria-label={label} value={value} onChange={(e) => onChange(Number(e.target.value))}
        className={cn("h-9 w-full min-w-0 appearance-none rounded-[10px] bg-muted px-3 text-[15px] font-semibold tabular-nums outline-none focus:ring-2 focus:ring-[#0a84ff]/50", className)}>
        {Array.from({ length: max + 1 }, (_, reps) => <option key={reps} value={reps}>{reps}</option>)}
    </select>
}
