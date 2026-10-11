'use client'
import { addTimeToDate, toTimeInputValue } from "@/utils/date";
import { DatePicker } from "./datePicker";
import { useEffect, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Dumbbell, Play, Repeat } from "lucide-react";
import { useEventStore } from "@/lib/zustand/eventStore";
import { createCalendarEvent, deleteCalendarEvent, updateCalendarEvent } from "./action";
import { getAllWorkouts, startSession } from "@/app/workouts/action";
import { Spinner } from "../ui/spinner";
import { DialogClose, DialogTitle } from "../ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { CalendarEvent, Recurrence } from "@/utils/types/calendarTypes";
import { Workout } from "@/utils/types/types";
import { useUser } from "@/components/user-provider";
import { InviteField } from "./InviteField";
import { Group } from "./Group";

/** The time inputs only show hours and minutes, the date needs the seconds too. */
const withSeconds = (time: string) => time.length === 5 ? `${time}:00` : time

const RECURRENCES: { label: string, value: Recurrence | null }[] = [
    { label: "Nie", value: null },
    { label: "Täglich", value: "DAILY" },
    { label: "Wöchentlich", value: "WEEKLY" },
    { label: "Monatlich", value: "MONTHLY" },
]

/** The last occurrence may start on this day, so the end is the end of the day. */
const endOfDay = (date: Date) => {
    const end = new Date(date)
    end.setHours(23, 59, 59, 0)
    return end
}

export function EventDialog({ calendarEvent, closeDialog, startDate }: { calendarEvent: CalendarEvent | null, closeDialog: () => void, startDate: Date | null }) {
    const setEvents = useEventStore((state) => state.setEvents)
    const reload = useEventStore((state) => state.reload)
    const events = useEventStore((state) => state.events)
    const endDate = new Date(startDate || new Date())
    endDate.setHours(endDate.getHours() + 1)


    const [event, setEvent] = useState<CalendarEvent>(calendarEvent || { id: "", title: "", startDate: startDate || new Date(), endDate, workoutId: null, creatorId: "", invitees: [], recurrence: null, recurrenceEnd: null });
    // the dates of a repeating event are the clicked occurrence's, the backend moves the series by as much as it was moved
    const [occurrenceStart] = useState(() => calendarEvent ? new Date(calendarEvent.startDate) : undefined)
    const user = useUser()
    // controlled, the workout fills an empty title and the date pickers keep the times
    const [title, setTitle] = useState(event.title)
    const [startTime, setStartTime] = useState(toTimeInputValue(event.startDate).slice(0, 5))
    const [endTime, setEndTime] = useState(toTimeInputValue(event.endDate).slice(0, 5))
    const [workouts, setWorkouts] = useState<Workout[] | null>(null)
    const router = useRouter()
    const [pending, run] = useTransition()

    useEffect(() => {
        getAllWorkouts().then(setWorkouts).catch(() => setWorkouts([]))
    }, [])

    const selectWorkout = (workoutId: number | null) => {
        const workout = workouts?.find((w) => w.id === workoutId)
        setEvent((prev) => ({ ...prev, workoutId, workoutName: workout?.name ?? null }))
        // an event without a title is named after its workout
        setTitle((prev) => prev || workout?.name || "")
    }

    /** Starts the planned workout and opens it, while another one is running that one is opened. */
    const startWorkout = () => run(async () => {
        await startSession(event.workoutId)
        router.push("/workouts/session")
    })

    const saveEvent = () => run(async () => {
        addTimeToDate(withSeconds(startTime), event.startDate)
        addTimeToDate(withSeconds(endTime), event.endDate)
        const saved = { ...event, title: title || event.workoutName || "" }

        if (!saved.id) {
            await createCalendarEvent(saved)
        } else {
            await updateCalendarEvent(saved, occurrenceStart)
        }
        // the occurrences of a repeating event, the creator's picture and the id come from the backend
        reload()
        closeDialog();
    })

    const deleteEvent = () => {
        const question = calendarEvent?.recurrence
            ? `Ereignis „${event.title || "Ohne Titel"}“ mit allen Wiederholungen löschen?`
            : `Ereignis „${event.title || "Ohne Titel"}“ löschen?`
        if (!event.id || !confirm(question)) return
        run(async () => {
            await deleteCalendarEvent(event.id)
            setEvents([...events.filter((e) => e.id !== event.id)])
            closeDialog();
        })
    }

    const workout = workouts?.find((w) => w.id === event.workoutId)
    // only a saved workout can be started, a changed selection has to be saved first
    const canStart = calendarEvent?.workoutId != null && event.workoutId === calendarEvent.workoutId

    return <form onSubmit={(e) => { e.preventDefault(); saveEvent() }} className="flex max-h-[85vh] flex-col">
        <header className="grid grid-cols-[1fr_auto_1fr] items-center px-4 pt-4 pb-3">
            <DialogClose type="button" className="justify-self-start text-[17px] text-[#0a84ff]">Abbrechen</DialogClose>
            <DialogTitle className="text-[17px] font-semibold">{event.id ? "Ereignis bearbeiten" : "Neues Ereignis"}</DialogTitle>
            <button type="submit" disabled={pending} className="justify-self-end text-[17px] font-semibold text-[#0a84ff] disabled:opacity-40">
                {pending ? <Spinner className="size-4" /> : event.id ? "Fertig" : "Hinzufügen"}
            </button>
        </header>

        <div className="flex flex-col gap-6 overflow-y-auto px-4 pb-6 pt-2">
            <Group>
                <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Titel" aria-label="Titel"
                    className="w-full bg-transparent px-4 py-3 text-[17px] outline-none placeholder:text-muted-foreground/60" />
            </Group>

            <Group>
                <Row label="Beginn">
                    <DatePicker date={event.startDate} setDate={(date) => setEvent((prev) => ({ ...prev, startDate: date }))} />
                    <TimeInput value={startTime} onChange={setStartTime} label="Uhrzeit Beginn" />
                </Row>
                <Row label="Ende">
                    <DatePicker date={event.endDate} setDate={(date) => setEvent((prev) => ({ ...prev, endDate: date }))} />
                    <TimeInput value={endTime} onChange={setEndTime} label="Uhrzeit Ende" />
                </Row>
            </Group>

            <Group>
                <Row label="Wiederholen" icon={<Repeat className="size-4" />}>
                    <Select
                        items={RECURRENCES}
                        value={event.recurrence}
                        onValueChange={(value) => setEvent((prev) => ({ ...prev, recurrence: value as Recurrence | null }))}
                    >
                        <SelectTrigger className="h-auto bg-transparent px-0 py-0 text-[15px] text-muted-foreground hover:text-foreground">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent align="end">
                            {RECURRENCES.map((r) => <SelectItem key={r.label} value={r.value}>{r.label}</SelectItem>)}
                        </SelectContent>
                    </Select>
                </Row>
                {event.recurrence && <Row label="Endet">
                    {event.recurrenceEnd
                        ? <>
                            <DatePicker date={event.recurrenceEnd} setDate={(date) => setEvent((prev) => ({ ...prev, recurrenceEnd: endOfDay(date) }))} />
                            <button type="button" onClick={() => setEvent((prev) => ({ ...prev, recurrenceEnd: null }))}
                                className="text-[15px] text-[#0a84ff]">Nie</button>
                        </>
                        : <button type="button" onClick={() => setEvent((prev) => ({ ...prev, recurrenceEnd: endOfDay(prev.startDate) }))}
                            className="text-[15px] text-muted-foreground hover:text-foreground">Nie</button>}
                </Row>}
            </Group>

            <Section title="Training">
                <Group>
                    <Row label="Vorlage" icon={<Dumbbell className="size-4" />}>
                        {workouts === null
                            ? <Spinner className="size-4 text-muted-foreground" />
                            : workouts.length === 0
                                ? <Link href="/workouts/new" className="text-[15px] text-[#0a84ff]">Vorlage erstellen</Link>
                                : <Select
                                    items={[{ label: "Keine", value: null }, ...workouts.map((w) => ({ label: w.name, value: w.id }))]}
                                    value={event.workoutId}
                                    onValueChange={(value) => selectWorkout(value as number | null)}
                                >
                                    <SelectTrigger className="h-auto max-w-48 bg-transparent px-0 py-0 text-[15px] text-muted-foreground hover:text-foreground">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent align="end">
                                        <SelectItem value={null}>Keine</SelectItem>
                                        {workouts.map((w) => <SelectItem key={w.id} value={w.id}>{w.name}</SelectItem>)}
                                    </SelectContent>
                                </Select>}
                    </Row>
                    {workout && workout.workoutExercises.length > 0 &&
                        <p className="px-4 py-3 text-[15px] text-muted-foreground">
                            {workout.workoutExercises.map((e) => e.exercise.name).join(" · ")}
                        </p>}
                    {canStart &&
                        <button type="button" onClick={startWorkout} disabled={pending}
                            className="flex w-full items-center gap-2 px-4 py-3 text-left text-[17px] text-[#30d158] hover:bg-muted/60 disabled:opacity-50">
                            <Play className="size-4 fill-current" />Training starten
                        </button>}
                </Group>
            </Section>

            <Section title="Eingeladene">
                <InviteField
                    invitees={event.invitees}
                    setInvitees={(invitees) => setEvent((prev) => ({ ...prev, invitees }))}
                    excludeIds={[event.creatorId || user?.id || ""]}
                />
            </Section>

            {event.id && <Group>
                <button type="button" onClick={deleteEvent} disabled={pending}
                    className="w-full px-4 py-3 text-center text-[17px] text-[#ff453a] hover:bg-muted/60 disabled:opacity-50">
                    Ereignis löschen
                </button>
            </Group>}
        </div>
    </form>
}

function Section({ title, children }: { title: string, children: React.ReactNode }) {
    return <section>
        <h3 className="px-4 pb-1.5 text-[13px] uppercase tracking-wide text-muted-foreground">{title}</h3>
        {children}
    </section>
}

function Row({ label, icon, children }: { label: string, icon?: React.ReactNode, children: React.ReactNode }) {
    return <div className="flex min-h-11 items-center gap-2 px-4 py-1.5">
        {icon && <span className="text-muted-foreground">{icon}</span>}
        <span className="flex-1 text-[17px]">{label}</span>
        {children}
    </div>
}

function TimeInput({ value, onChange, label }: { value: string, onChange: (value: string) => void, label: string }) {
    return <input type="time" step={60} value={value} onChange={(e) => onChange(e.target.value)} aria-label={label}
        className="rounded-md bg-muted px-2.5 py-1 text-[15px] tabular-nums outline-none focus:text-[#0a84ff] [&::-webkit-calendar-picker-indicator]:hidden" />
}
