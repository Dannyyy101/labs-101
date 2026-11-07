'use client'

import { cn } from "@/lib/utils"
import { useState } from "react"

/** Number field that can be emptied while typing, it falls back to 0 when left empty. */
export default function NumberInput({ value, onChange, label, decimal, className }: { value: number, onChange: (value: number) => void, label: string, decimal?: boolean, className?: string }) {
    const [text, setText] = useState<string | null>(null)
    return <input
        aria-label={label}
        inputMode={decimal ? "decimal" : "numeric"}
        value={text ?? String(value).replace(".", ",")}
        onFocus={(e) => e.target.select()}
        onChange={(e) => {
            const raw = e.target.value.replace(/[^\d.,]/g, "")
            setText(raw)
            const parsed = parseFloat(raw.replace(",", "."))
            onChange(Number.isFinite(parsed) ? parsed : 0)
        }}
        onBlur={() => setText(null)}
        className={cn("glass-inset h-9 w-full min-w-0 rounded-[10px] px-3 text-[14px] font-semibold tabular-nums outline-none focus:ring-2 focus:ring-brand/50", className)}
    />
}
