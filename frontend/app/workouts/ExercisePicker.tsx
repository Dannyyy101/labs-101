'use client'

import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Exercise } from "@/utils/types/workoutTypes"
import { Plus, Search } from "lucide-react"
import { useMemo, useState } from "react"
import { MUSCLES } from "./stats"

/** Search through all exercises and pick one, the dialog stays open to add several in a row. */
export default function ExercisePicker({ exercises, onPick, children, className, title = "Übung hinzufügen" }: {
    exercises: Exercise[], onPick: (exercise: Exercise) => void, children: React.ReactNode, className?: string, title?: string
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
                    <DialogTitle>{title}</DialogTitle>
                </DialogHeader>
                <label className="flex items-center gap-2 rounded-[10px] glass-inset px-3 py-2">
                    <Search className="size-4 text-ink-muted" />
                    <input autoFocus value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Übung oder Muskel suchen"
                        className="w-full bg-transparent text-[14px] outline-none placeholder:text-ink-muted" />
                </label>
                <div className="-mx-2 flex max-h-[50vh] flex-col overflow-y-auto">
                    {found.map((exercise) => {
                        const count = added.filter((id) => id === exercise.id).length
                        const muscles = [...new Set(exercise.bodyParts.map((p) => p.slug).filter(Boolean))]
                        return <button key={exercise.id} type="button" onClick={() => {
                            onPick(exercise)
                            setAdded([...added, exercise.id])
                        }} className="flex items-center gap-3 rounded-[12px] px-2 py-2 text-left hover:bg-surface-raised/50 dark:hover:bg-white/5">
                            <div className="min-w-0 flex-1">
                                <div className="truncate text-[14px] font-semibold">{exercise.name}</div>
                                <div className="truncate text-label text-ink-muted">
                                    {muscles.length ? muscles.map((m) => MUSCLES[m!] ?? m).join(", ") : exercise.description || exercise.type}
                                </div>
                            </div>
                            {count > 0
                                ? <span className="rounded-full bg-success-soft px-2 py-0.5 text-label font-semibold text-success">{count}× hinzugefügt</span>
                                : <Plus className="size-4 text-brand-strong" />}
                        </button>
                    })}
                    {!found.length && <p className="py-8 text-center text-body text-ink-muted">
                        {exercises.length ? "Keine passende Übung" : "Noch keine Übungen – lege sie unter „Exercises“ an"}
                    </p>}
                </div>
            </DialogContent>
        </Dialog>
    </>
}
