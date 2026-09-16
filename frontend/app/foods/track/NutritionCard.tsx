import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";

export interface NutritionCardProps {
    name: string,
    color: string
    value: number
    goal: number
}

export const colorMap: Record<string, string> = {
    red: 'bg-protein',
    orange: 'bg-carbohydrates',
    blue: 'bg-fat',
};

export default function NutritionCard({ props }: { props: NutritionCardProps }) {
    return <Card className="w-64 md:w-96">
        <CardHeader>
            <CardDescription className="break-all h-10">{props.name}</CardDescription>
            <CardTitle className="text-2xl">{Math.round(props.value * 10) / 10}&#8202;g</CardTitle>
        </CardHeader>
        <CardContent>
            <p>Goal {props.goal}</p>
            <Progress className={`${colorMap[props.color]} rounded-md`} value={props.value / props.goal * 100} />
        </CardContent>
    </Card>
}