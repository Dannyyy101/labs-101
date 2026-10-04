import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import NutritionCard from "./NutritionCard";

export interface CalorieCardProps {
    color: string
    consumed: number
    goal: number
    burned: number
}

export default function CalorieCard({ props }: { props: CalorieCardProps }) {
    return <div className="flex flex items-center w-full gap-x-4">
        <CircularProgress value={props.consumed} max={props.goal} className={props.consumed > props.goal ? "stroke-destructive" : "stroke-amber-500"} >
            <h3 className="text-2xl font-semibold">{props.goal - props.consumed}</h3>
            <p>kcal übrig</p>
        </CircularProgress>
        <div className="flex flex-col gap-1 w-full h-fit">
            <div className="flex justify-between w-full">
                <p className="text-muted-foreground text-sm">Ziel</p>
                <p className="font-semibold">{props.goal}</p>
            </div>
            <div className="flex justify-between w-full">
                <p className="text-muted-foreground text-sm">Gegessen</p>
                <p className="font-semibold">{props.consumed}</p>
            </div>
            <div className="flex justify-between w-full">
                <p className="text-muted-foreground text-sm">Verbrannt</p>
                <p className="font-semibold">+{props.burned}</p>
            </div>
        </div>
    </div>

}

function CircularProgress({
    value, max, size = 116, strokeWidth = 10, className, children,
}: { value: number; max: number; size?: number; strokeWidth?: number; className?: string; children?: React.ReactNode }) {
    const r = (size - strokeWidth) / 2
    const c = 2 * Math.PI * r
    const pct = Math.min(value / max, 1)

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