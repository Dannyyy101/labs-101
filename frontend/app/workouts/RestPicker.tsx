'use client'

import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { cn } from "@/lib/utils"
import { formatRest, REST_OPTIONS } from "./stats"

/** The rest after a set, picked from the usual durations (a stored odd one stays selectable). */
export default function RestPicker({ value, onChange, label, className }: { value: number, onChange: (seconds: number) => void, label: string, className?: string }) {
    const options = REST_OPTIONS.includes(value) ? REST_OPTIONS : [...REST_OPTIONS, value].sort((a, b) => a - b)
    return <DropdownMenu>
        <DropdownMenuTrigger aria-label={`${label}: ${formatRest(value)}`} title={label}
            className={cn("h-9 w-full min-w-0 rounded-[10px] bg-muted px-2 text-center text-[15px] font-semibold tabular-nums outline-none focus-visible:ring-2 focus-visible:ring-[#0a84ff]/50",
                !value && "text-muted-foreground", className)}>
            {formatRest(value)}
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-36">
            <DropdownMenuGroup>
                <DropdownMenuLabel>Ruhezeit nach dem Satz</DropdownMenuLabel>
                <DropdownMenuRadioGroup value={value} onValueChange={(v) => onChange(v as number)}>
                    {options.map((seconds) => <DropdownMenuRadioItem key={seconds} value={seconds} className="tabular-nums">{formatRest(seconds)}</DropdownMenuRadioItem>)}
                </DropdownMenuRadioGroup>
            </DropdownMenuGroup>
        </DropdownMenuContent>
    </DropdownMenu>
}
