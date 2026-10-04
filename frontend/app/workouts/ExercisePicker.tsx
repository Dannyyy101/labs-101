'use client'

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Exercise } from "@/utils/types/workoutTypes"
import { Plus, Search } from "lucide-react"
import { useMemo, useState } from "react"
import { MUSCLES } from "./stats"

/** Search through all exercises and pick one, the dialog stays open to add several in a row. */
export default function ExercisePicker({ exercises, onPick, children, className }: {
    exercises: Exercise[], onPick: (exercise: Exercise) => void, children: React.ReactNode, className?: string
}) {
    const [open, setOpen] = useState(false)
    const [query, setQuery] = useState("")
    const [added, setAdded] = useState<number[]>([])

    const found = useMemo(() => {
        const q = query.trim().toLowerCase()
        return exercises.filter((e) => !q || e.name.toLowerCase().includes(q) || e.bodyParts.some((p) => p.slug && (MUSCLES[p.slug] ?? p.slug).toLowerCase().includes(q)))
    }, [exercises, query])

    const changeOpen = (value: boolean) => {
        setOpen(value)
        if (value) {
            setQuery("")
            setAdded([])
        }
    }

    return <>
        <button type="button" onClick={() => changeOpen(true)} className={className}>{children}</button>
        <Dialog open={open} onOpenChange={changeOpen}>
            <DialogContent className="gap-4 sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>Übung hinzufügen</DialogTitle>
                </DialogHeader>
                <label className="flex items-center gap-2 rounded-[10px] bg-muted px-3 py-2">
                    <Search className="size-4 text-muted-foreground" />
                    <input autoFocus value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Übung oder Muskel suchen"
                        className="w-full bg-transparent text-[15px] outline-none placeholder:text-muted-foreground" />
                </label>
                <div className="-mx-2 flex max-h-[50vh] flex-col overflow-y-auto">
                    {found.map((exercise) => {
                        const count = added.filter((id) => id === exercise.id).length
                        const muscles = [...new Set(exercise.bodyParts.map((p) => p.slug).filter(Boolean))]
                        return <button key={exercise.id} type="button" onClick={() => {
                            onPick(exercise)
                            setAdded([...added, exercise.id])
                        }} className="flex items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-muted">
                            <div className="min-w-0 flex-1">
                                <div className="truncate text-[15px] font-semibold">{exercise.name}</div>
                                <div className="truncate text-xs text-muted-foreground">
                                    {muscles.length ? muscles.map((m) => MUSCLES[m!] ?? m).join(", ") : exercise.description || exercise.type}
                                </div>
                            </div>
                            {count > 0
                                ? <span className="rounded-full bg-[#30d158]/15 px-2 py-0.5 text-xs font-semibold text-[#30d158]">{count}× hinzugefügt</span>
                                : <Plus className="size-4 text-[#0a84ff]" />}
                        </button>
                    })}
                    {!found.length && <p className="py-8 text-center text-sm text-muted-foreground">
                        {exercises.length ? "Keine passende Übung" : "Noch keine Übungen – lege sie unter „Exercises“ an"}
                    </p>}
                </div>
            </DialogContent>
        </Dialog>
    </>
}
