import { de } from "@/app/runs/format"
import { cn } from "@/lib/utils"

// building blocks of the run and workout dashboards

export function Segmented<T extends string>({ value, options, onChange }: { value: T, options: { value: T, label: string }[], onChange: (value: T) => void }) {
    return <div className="ml-auto flex whitespace-nowrap rounded-[9px] bg-muted p-0.5 text-sm font-medium">
        {options.map((o) => <button key={o.value} onClick={() => onChange(o.value)}
            className={cn("rounded-[7px] px-3.5 py-1", o.value === value
                ? "bg-background font-semibold text-foreground shadow-[0_1px_3px_rgba(0,0,0,.12)]"
                : "text-muted-foreground")}>
            {o.label}
        </button>)}
    </div>
}

export function Card({ className, children }: { className?: string, children: React.ReactNode }) {
    return <div className={cn("min-w-0 overflow-hidden rounded-[18px] bg-card p-5 shadow-[0_1px_2px_rgba(0,0,0,.04),0_8px_24px_rgba(0,0,0,.05)] dark:shadow-none", className)}>
        {children}
    </div>
}

export function CardHead({ title, right, small, className }: { title: string, right?: React.ReactNode, small?: boolean, className?: string }) {
    return <div className={cn("mb-3.5 flex flex-wrap items-baseline gap-x-2.5 gap-y-1", className)}>
        <h2 className={cn("whitespace-nowrap font-semibold", small ? "text-[15px]" : "text-[17px]")}>{title}</h2>
        {right && <div className="ml-auto min-w-0 text-sm text-muted-foreground">{right}</div>}
    </div>
}

export function Stat({ label, value, unit }: { label: string, value: string, unit?: string }) {
    return <div>
        <dt className="text-sm font-medium text-muted-foreground">{label}</dt>
        <dd className="mt-0.5 text-[22px] font-semibold tabular-nums">{value} {unit && <small className="text-sm font-medium text-muted-foreground">{unit}</small>}</dd>
    </div>
}

export function Delta({ current, previous, lowerIsBetter, format }: { current: number, previous: number, lowerIsBetter?: boolean, format?: (c: number, p: number) => string }) {
    if (!previous) return <div className="mt-1.5 text-sm">&nbsp;</div>
    const change = (current - previous) / previous * 100
    if (Math.abs(change) < 1) return <div className="mt-1.5 text-sm font-medium text-muted-foreground">gleich wie davor</div>
    const good = lowerIsBetter ? change < 0 : change > 0
    return <div className={cn("mt-1.5 text-sm font-medium", good ? "text-[#30d158]" : "text-[#ff453a]")}>
        {change > 0 ? "▲" : "▼"} {format ? format(current, previous) : `${de(Math.abs(change))} %`} <span className="font-normal text-muted-foreground">ggü. davor</span>
    </div>
}

export const Note = ({ children, className }: { children?: React.ReactNode, className?: string }) =>
    <div className={cn("mt-1.5 text-sm font-medium text-muted-foreground", className)}>{children ?? " "}</div>

/** A row of KPI cards, 2 per row on phones and 5 in one row on large screens. */
export function KpiGrid({ items }: { items: { label: string, color: string, value: React.ReactNode, delta: React.ReactNode }[] }) {
    return <div className="grid grid-cols-2 gap-4 md:grid-cols-6 lg:grid-cols-5">
        {items.map((item, i) => <Card key={item.label} className={cn(
            "md:col-span-2 lg:col-span-1",
            i >= 3 && "md:col-span-3",
            i === 4 && "col-span-2",
        )}>
            <div className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
                <span className="size-2 rounded-full" style={{ background: item.color }} />{item.label}
            </div>
            <div className="mt-1.5 text-2xl font-bold leading-tight tracking-tight tabular-nums md:text-[30px] [&_small]:text-[15px] [&_small]:font-medium [&_small]:tracking-normal [&_small]:text-muted-foreground">
                {item.value}
            </div>
            {item.delta}
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
