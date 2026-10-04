import { formatNumber } from "./format";

export interface NutritionCardProps {
    name: string,
    color: string
    consumed: number
    goal: number
}

export const colorMap: Record<string, string> = {
    protein: 'bg-protein',
    carbohydrates: 'bg-carbohydrates',
    fat: 'bg-fat',
};

export default function NutritionCard({ props }: { props: NutritionCardProps }) {
    const pct = props.goal > 0 ? Math.min(props.consumed / props.goal, 1) * 100 : 0

    return <div>
        <div className="flex items-baseline justify-between gap-x-2">
            <span className="font-semibold">{props.name}</span>
            <span className="tabular-nums whitespace-nowrap">
                <span className="font-semibold">{formatNumber(props.consumed)} g</span>
                <span className="text-muted-foreground"> / {formatNumber(props.goal)} g</span>
            </span>
        </div>
        <div className="h-2.5 rounded-full bg-muted mt-1.5 overflow-hidden" role="progressbar" aria-valuenow={Math.round(props.consumed)} aria-valuemax={props.goal} aria-label={props.name}>
            <div className={`h-full rounded-full transition-[width] duration-500 ${colorMap[props.color]}`} style={{ width: `${pct}%` }} />
        </div>
    </div>
}
