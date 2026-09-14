'use client'
import { TableHeader, TableRow, TableHead, TableBody, TableCell, Table } from "@/components/ui/table";
import { Food, FoodWithPortion } from "@/utils/types/food";
import { useState } from "react";
import EditFood from "./EditFood";

export default function FoodTable({ foods }: { foods: FoodWithPortion[] }) {

    const [selected, setSelected] = useState<FoodWithPortion>()

    return <> <Table>
        <TableHeader className="sticky top-0 z-10 bg-background">
            <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Protein</TableHead>
                <TableHead>kcal</TableHead>
                <TableHead className="text-right">Price</TableHead>
            </TableRow>
        </TableHeader>
        <TableBody>
            {foods.map((food) => (
                <TableRow key={food.id} onClick={() => setSelected(food)}>
                    <TableCell className="font-medium">{food.name}</TableCell>
                    <TableCell>{food.protein}</TableCell>
                    <TableCell>{food.kcal}</TableCell>
                    <TableCell className="text-right">$250.00</TableCell>
                </TableRow>
            ))}
        </TableBody>
    </Table>
        <EditFood food={selected} close={() => setSelected(undefined)} />
    </>
}