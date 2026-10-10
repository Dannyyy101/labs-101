'use client'
import { TableHeader, TableRow, TableHead, TableBody, TableCell, Table } from "@/components/ui/table";
import { Food, FoodWithPortion } from "@/utils/types/food";
import { useState } from "react";
import EditFood from "./EditFood";

const formatValue = (value: number | null | undefined) => (value ?? 0).toLocaleString("de-DE", { maximumFractionDigits: 1 })

export default function FoodTable({ foods }: { foods: FoodWithPortion[] }) {

    const [selected, setSelected] = useState<FoodWithPortion>()

    return <> <Table>
        <TableHeader className="sticky top-0 z-10 bg-background">
            <TableRow>
                <TableHead>Name</TableHead>
                <TableHead className="w-24 text-right">Protein</TableHead>
                <TableHead className="w-24 text-right">kcal</TableHead>
            </TableRow>
        </TableHeader>
        <TableBody>
            {foods.map((food) => (
                <TableRow key={food.id} onClick={() => setSelected(food)} className="cursor-pointer">
                    <TableCell className="font-medium whitespace-normal">{food.name}</TableCell>
                    <TableCell className="text-right tabular-nums">{formatValue(food.protein)} g</TableCell>
                    <TableCell className="text-right tabular-nums">{formatValue(food.kcal)}</TableCell>
                </TableRow>
            ))}
        </TableBody>
    </Table>
        <EditFood food={selected} close={() => setSelected(undefined)} />
    </>
}