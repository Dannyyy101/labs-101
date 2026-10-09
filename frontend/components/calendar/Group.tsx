import { cn } from "@/lib/utils"

/** White inset group with hairlines between the rows, like an iOS list. */
export function Group({ className, children }: { className?: string, children: React.ReactNode }) {
    return <div className={cn("divide-y divide-border/70 overflow-hidden rounded-xl bg-background", className)}>{children}</div>
}
