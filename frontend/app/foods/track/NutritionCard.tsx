import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";

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
    return <div>
        <div className="flex justify-between">{props.name}
            <span className="tabular-nums">
                <span className="font-semibold">{props.consumed}g</span> / {props.goal}g
            </span>
        </div>
        <Progress className={`${colorMap[props.color]} rounded-md`} value={props.consumed / props.goal * 100} />
    </div>

}