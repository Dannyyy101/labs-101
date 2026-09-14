import { Card, CardHeader, CardDescription, CardTitle, CardContent } from "@/components/ui/card";
import { colorMap } from "./NutritionCard";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

export interface CalorieCardProps {
    name: string,
    color: string
    consumed: number
    goal: number
    burned: number
}

export default function CalorieCard({ props }: { props: CalorieCardProps }) {
    return <Card className="w-full flex items-center flex-row">
        <CircularProgress value={props.consumed} max={props.goal} className={props.consumed > props.goal ? "stroke-destructive" : "stroke-primary"} >
            <h3 className="text-2xl font-semibold">{props.goal - props.consumed}</h3>
            <p>left</p>
        </CircularProgress>
        <div className="flex flex-col gap-y-2 w-full">
            <div>
                <CardDescription>Consumption today</CardDescription>
                <CardTitle className="text-xl -mt-1">{props.consumed} of {props.goal}&#8202;kcal</CardTitle>
            </div>
            <div className="flex max-w-96 justify-between">
                <div>
                    <CardDescription>Goal</CardDescription>
                    <p className="text-lg font-semibold -mt-1.5">{props.goal}</p>
                </div>
                <div>
                    <CardDescription>Consumed</CardDescription>
                    <p className="text-lg font-semibold -mt-1.5">{props.consumed}</p>
                </div>
                <div>
                    <CardDescription>Burned</CardDescription>
                    <p className="text-lg font-semibold -mt-1.5">{props.burned}</p>
                </div>
            </div>
        </div>
    </Card>
}

function CircularProgress({
    value, max, size = 116, strokeWidth = 10, className, children,
}: { value: number; max: number; size?: number; strokeWidth?: number; className?: string; children?: React.ReactNode }) {
    const r = (size - strokeWidth) / 2
    const c = 2 * Math.PI * r
    const pct = Math.min(value / max, 1)

    return (
        <div className="ml-8 relative shrink-0" style={{ width: size, height: size }}>
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