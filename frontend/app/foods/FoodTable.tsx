'use client'
import { TableHeader, TableRow, TableHead, TableBody, TableCell, Table } from "@/components/ui/table";
import { Food, FoodWithPortion } from "@/utils/types/food";
import { useState } from "react";
import EditFood from "./EditFood";

const formatValue = (value: number | null | undefined) => (value ?? 0).toLocaleString("de-DE", { maximumFractionDigits: 1 })

// the list comes without portions, EditFood starts with none
export default function FoodTable({ foods }: { foods: Food[] }) {

    const [selected, setSelected] = useState<FoodWithPortion>()

    return <> <Table>
        {/* sticky header in thick glass, the rows scroll underneath */}
        <TableHeader className="glass-thick sticky top-0 z-10 rounded-none border-x-0 border-t-0 shadow-none [&_tr]:border-line">
            <TableRow className="hover:bg-transparent">
                <TableHead className="h-9 pl-4 text-label text-ink-muted">Name</TableHead>
                <TableHead className="h-9 w-24 text-right text-label text-ink-muted">Protein</TableHead>
                <TableHead className="h-9 w-24 pr-4 text-right text-label text-ink-muted">Energie</TableHead>
            </TableRow>
        </TableHeader>
        <TableBody>
            {foods.map((food) => (
                <TableRow key={food.id} onClick={() => setSelected({ ...food, portions: [] })}
                    className="cursor-pointer border-line hover:bg-surface-raised/50 dark:hover:bg-white/5">
                    <TableCell className="whitespace-normal py-2.5 pl-4 font-semibold">{food.name}</TableCell>
                    <TableCell className="py-2.5 text-right tabular-nums">{formatValue(food.protein)} <span className="text-ink-muted">g</span></TableCell>
                    <TableCell className="py-2.5 pr-4 text-right tabular-nums">{formatValue(food.kcal)} <span className="text-ink-muted">kcal</span></TableCell>
                </TableRow>
            ))}
            {!foods.length && <TableRow className="hover:bg-transparent">
                <TableCell colSpan={3} className="py-10 text-center text-ink-muted">Kein Lebensmittel gefunden. Lege es über „Lebensmittel anlegen“ an.</TableCell>
            </TableRow>}
        </TableBody>
    </Table>
        <EditFood food={selected} close={() => setSelected(undefined)} />
    </>
}