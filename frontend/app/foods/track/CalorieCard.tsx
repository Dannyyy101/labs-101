import { cn } from "@/lib/utils";
import { formatNumber } from "./format";

export interface CalorieCardProps {
    consumed: number
    goal: number
    burned: number
}

export default function CalorieCard({ props }: { props: CalorieCardProps }) {
    const remaining = props.goal - props.consumed + props.burned

    return <div className="flex items-center w-full gap-x-6">
        <CircularProgress value={props.consumed} max={props.goal + props.burned} className={remaining < 0 ? "stroke-destructive" : "stroke-orange-400"}>
            <p className="text-3xl font-bold tabular-nums">{formatNumber(remaining)}</p>
            <p className="text-sm text-muted-foreground">kcal übrig</p>
        </CircularProgress>
        <dl className="flex flex-col gap-y-2">
            <Stat label="Ziel" value={formatNumber(props.goal)} />
            <Stat label="Gegessen" value={formatNumber(props.consumed)} />
            <Stat label="Verbrannt" value={`+${formatNumber(props.burned)}`} />
        </dl>
    </div>
}

function Stat({ label, value }: { label: string, value: string }) {
    return <div>
        <dt className="text-sm text-muted-foreground">{label}</dt>
        <dd className="text-2xl font-semibold tabular-nums leading-tight">{value}</dd>
    </div>
}

function CircularProgress({
    value, max, size = 150, strokeWidth = 16, className, children,
}: { value: number; max: number; size?: number; strokeWidth?: number; className?: string; children?: React.ReactNode }) {
    const r = (size - strokeWidth) / 2
    const c = 2 * Math.PI * r
    const pct = max > 0 ? Math.min(value / max, 1) : 0

    return (
        <div className="relative shrink-0" style={{ width: size, height: size }}>
            <svg width={size} height={size} className="-rotate-90" role="img" aria-label={`${Math.round(pct * 100)}%`}>
                <circle cx={size / 2} cy={size / 2} r={r} strokeWidth={strokeWidth} fill="none" className="stroke-muted" />
                <circle
                    cx={size / 2} cy={size / 2} r={r} strokeWidth={strokeWidth} fill="none" strokeLinecap="round"
                    strokeDasharray={c} strokeDashoffset={c * (1 - pct)}
                    className={cn("transition-[stroke-dashoffset] duration-500", className)}
                />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">{children}</div>
        </div>
    )
}
