import { de } from "@/app/runs/format"
import { cn } from "@/lib/utils"
import { ArrowDown, ArrowUp, type LucideIcon } from "lucide-react"

// building blocks of the run and workout dashboards

export function Segmented<T extends string>({ value, options, onChange }: { value: T, options: { value: T, label: string }[], onChange: (value: T) => void }) {
    return <div role="tablist" className="glass ml-auto flex gap-0.5 whitespace-nowrap rounded-[12px] p-[3px]">
        {options.map((o) => <button key={o.value} role="tab" aria-selected={o.value === value} onClick={() => onChange(o.value)}
            className={cn("h-8 rounded-[9px] px-3.5 text-label font-semibold transition-colors", o.value === value
                ? "bg-surface-raised text-ink shadow-xs dark:bg-white/15"
                : "text-ink-muted hover:text-ink")}>
            {o.label}
        </button>)}
    </div>
}

/** A glyph in a soft brand tile, `solid` fills it with the brand for the selected or running object. */
export function IconTile({ icon: Icon, solid, size = "md", className }: { icon: LucideIcon, solid?: boolean, size?: "sm" | "md", className?: string }) {
    return <div className={cn("grid flex-none place-items-center rounded-[10px] transition-colors",
        size === "sm" ? "size-9" : "size-11",
        solid ? "bg-brand text-on-brand" : "bg-brand-soft text-brand",
        className)}>
        <Icon className={size === "sm" ? "size-[18px]" : "size-[22px]"} strokeWidth={2} />
    </div>
}

/** Classes of a soft status chip, add the tone: "bg-success-soft text-success", "bg-brand-soft text-brand-strong", … */
export const chip = "inline-flex h-[22px] items-center gap-1 rounded-[6px] px-2 text-label font-semibold whitespace-nowrap"

/** Neutral chip on glass, e.g. a set "8 × 60 kg". */
export const neutralChip = "bg-ink/[0.06] text-ink dark:bg-white/10"

/** A floating pane of liquid glass. */
export function Card({ className, children }: { className?: string, children: React.ReactNode }) {
    return <div className={cn("glass min-w-0 overflow-hidden rounded-[14px] p-4", className)}>
        {children}
    </div>
}

export function CardHead({ title, right, small, className }: { title: string, right?: React.ReactNode, small?: boolean, className?: string }) {
    return <div className={cn("mb-3 flex flex-wrap items-baseline gap-x-2.5 gap-y-1", className)}>
        <h2 className={cn("whitespace-nowrap text-ink", small ? "text-title" : "text-heading")}>{title}</h2>
        {right && <div className="ml-auto min-w-0 text-label text-ink-muted">{right}</div>}
    </div>
}

export function Stat({ label, value, unit }: { label: string, value: string, unit?: string }) {
    return <div>
        <dt className="text-label text-ink-muted">{label}</dt>
        <dd className="mt-0.5 text-[20px] font-bold leading-6 tracking-tight tabular-nums">{value} {unit && <small className="text-label text-ink-muted">{unit}</small>}</dd>
    </div>
}

export function Delta({ current, previous, lowerIsBetter, format }: { current: number, previous: number, lowerIsBetter?: boolean, format?: (c: number, p: number) => string }) {
    if (!previous) return <Note />
    const change = (current - previous) / previous * 100
    if (Math.abs(change) < 1) return <Note>gleich wie davor</Note>
    const good = lowerIsBetter ? change < 0 : change > 0
    const Arrow = change > 0 ? ArrowUp : ArrowDown
    return <div className="mt-1 flex items-center gap-1 text-caption text-ink-muted">
        <span className={cn("inline-flex items-center gap-0.5 font-semibold", good ? "text-success" : "text-danger")}>
            <Arrow className="size-3" strokeWidth={2.5} />
            {change > 0 ? "+" : "−"}{format ? format(current, previous) : `${de(Math.abs(change))} %`}
        </span>
        ggü. davor
    </div>
}

export const Note = ({ children, className }: { children?: React.ReactNode, className?: string }) =>
    <div className={cn("mt-1 text-caption text-ink-muted", className)}>{children ?? "\u00a0"}</div>

/** A row of glass KPI cards, 2 per row on phones and 5 in one row on large screens. */
export function KpiGrid({ items }: { items: { label: string, icon: LucideIcon, value: React.ReactNode, delta: React.ReactNode }[] }) {
    return <div className="grid grid-cols-2 gap-3 md:grid-cols-6 lg:grid-cols-5">
        {items.map((item, i) => <Card key={item.label} className={cn(
            "flex flex-col gap-2.5 p-3 sm:flex-row sm:items-center sm:gap-3 md:col-span-2 lg:col-span-1",
            i >= 3 && "md:col-span-3",
            i === 4 && "col-span-2",
        )}>
            <div className="grid size-9 flex-none place-items-center rounded-[10px] bg-brand-soft text-brand sm:size-11">
                <item.icon className="size-[18px] sm:size-[22px]" strokeWidth={2} />
            </div>
            <div className="min-w-0">
                <div className="truncate text-label text-ink">{item.label}</div>
                <div className="truncate text-kpi tabular-nums [&_small]:text-body [&_small]:font-semibold [&_small]:tracking-normal [&_small]:text-ink-muted">
                    {item.value}
                </div>
                {item.delta}
            </div>
        </Card>)}
    </div>
}

export type Period = "7" | "30" | "year"

export const PERIODS: { value: Period, label: string }[] = [
    { value: "7", label: "7 Tage" },
    { value: "30", label: "30 Tage" },
    { value: "year", label: "Jahr" },
]

// [from, to] of the period, `back` periods before the current one
export function periodRange(period: Period, back = 0): [Date, Date] {
    const now = new Date()
    if (period === "year") return [new Date(now.getFullYear(), 0, 1), now]
    const days = Number(period)
    const to = new Date(now.getFullYear(), now.getMonth(), now.getDate() - days * back, 23, 59, 59, 999)
    const from = new Date(to.getFullYear(), to.getMonth(), to.getDate() - days + 1)
    return [from, to]
}

export const isInPeriod = (date: string, [from, to]: [Date, Date]) => {
    const d = new Date(date)
    return d >= from && d <= to
}
