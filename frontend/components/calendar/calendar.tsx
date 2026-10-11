'use client'
import {
    Dialog,
    DialogContent, DialogTrigger
} from "@/components/ui/dialog";
import { useEventStore } from '@/lib/zustand/eventStore';
import { useEffect, useState } from 'react';

import { EventDialog } from './EventDialog';
import { CalendarEvent } from '@/utils/types/calendarTypes';
import { getAllCalendarEvents, getExternalCalendarEvents } from './action';
import { Button } from "../ui/button";
import { ChevronRight, ChevronLeft, Dumbbell, Repeat } from "lucide-react";
import { startAtTime } from "@/utils/date";
import { AvatarStack } from "./UserAvatar";


export interface Training {
    id: string
    name: string,
    description: string
    duration: number
}

/** Höhe einer Stunde in px; kleiner = weiter rausgezoomt. */
const HOUR_HEIGHT = 48

/** Setzt die Uhrzeit auf 00:00:00.000 und gibt ein neues Date zurück. */
const startOfDay = (date: Date) => {
    const newDate = new Date(date)
    newDate.setHours(0, 0, 0, 0)
    return newDate
}

/**
 * Montag der Woche, in der `date` liegt.
 * getDay(): 0 = Sonntag, 1 = Montag ... 6 = Samstag
 * (getDay() + 6) % 7 ergibt den Abstand zum Montag.
 */
const startOfWeek = (date: Date) => {
    const newDate = startOfDay(date)
    newDate.setDate(newDate.getDate() - ((newDate.getDay() + 6) % 7))
    return newDate
}

function useWindowSize() {
    const [windowSize, setWindowSize] = useState<{ width?: number, height?: number }>({
        width: undefined,
        height: undefined,
    });

    useEffect(() => {
        function handleResize() {
            setWindowSize({
                width: window.innerWidth,
                height: window.innerHeight,
            });
        }

        window.addEventListener("resize", handleResize);

        handleResize();

        return () => window.removeEventListener("resize", handleResize);
    }, []);
    return windowSize;
}

export default function Calendar() {
    const size = useWindowSize();
    // Erst nach dem Mount bekannt – vorher darf nicht gefetcht werden.
    const isSizeKnown = size.width !== undefined
    const STEPS = isSizeKnown ? (size.width! > 700 ? 7 : 1) : 1

    const [dateRange, setDateRange] = useState<Date>(() => startOfDay(new Date()))

    // Sobald die Breite feststeht bzw. sich die Ansicht ändert:
    // Wochenansicht auf Montag ausrichten, Tagesansicht auf den Tagesanfang.
    useEffect(() => {
        if (!isSizeKnown) return

        setDateRange((prev) => {
            const next = STEPS === 7 ? startOfWeek(prev) : startOfDay(prev)
            return next.getTime() === prev.getTime() ? prev : next
        })
    }, [STEPS, isSizeKnown])

    const increaseDateByDays = (date: Date, days: number) => {
        const newDate = new Date(date)
        newDate.setDate(date.getDate() + days)
        return newDate
    }

    const setEvents = useEventStore((state) => state.setEvents)
    const version = useEventStore((state) => state.version)

    useEffect(() => {
        if (!isSizeKnown) return

        const fetch = async () => {
            // the Apple calendars are slower, they are loaded from Apple when they weren't for a while
            const [own, external] = await Promise.all([getAllCalendarEvents(dateRange, STEPS), getExternalCalendarEvents(dateRange, STEPS)])
            setEvents([...(own.value || []), ...external])
        }

        fetch()
    }, [dateRange, STEPS, isSizeKnown, version])

    const WEEKDAYS = ['So.', 'Mo.', 'Di.', 'Mi.', 'Do.', 'Fr.', 'Sa.']

    const pad = (value: number) => value.toString().padStart(2, '0')

    const formatColumnDate = (date: Date) =>
        `${WEEKDAYS[date.getDay()]} ${pad(date.getDate())}.${pad(date.getMonth() + 1)}.`


    return (
        <div className="px-4 w-full relative">
            <div className="absolute right-4 z-20 flex">
                <Button variant="secondary" onClick={() => setDateRange((prev) => increaseDateByDays(prev, -STEPS))}><ChevronLeft /></Button>
                <Button variant="secondary" onClick={() => setDateRange((prev) => increaseDateByDays(prev, STEPS))}><ChevronRight /></Button>
            </div>

            <div className="mt-10 grid min-h-10" style={{ gridTemplateColumns: `60px repeat(${STEPS}, minmax(0, 1fr))` }}>
                <div></div>
                {Array(STEPS).fill(0).map((_, index) => (
                    <div key={index} className="sticky top-0 z-10 flex min-w-0 flex-col items-center justify-center gap-1 pb-1">
                        <p className="h-10 content-center text-center whitespace-nowrap truncate">{formatColumnDate(increaseDateByDays(dateRange, index))}</p>
                        <AllDayEvents columnDate={increaseDateByDays(dateRange, index)} />
                    </div>
                ))}
            </div>
            <div className="grid overflow-y-auto max-h-150" style={{ gridTemplateColumns: `60px repeat(${STEPS}, minmax(0, 1fr))` }}>
                <Time />

                {Array.from(Array(STEPS).keys()).map((index) => {
                    const columnDate = increaseDateByDays(dateRange, index);
                    return <Day columnDate={columnDate} key={index} />
                })}
            </div>
        </div>
    );
}
const Time = () => {
    return <div className="grid" style={{ gridTemplateRows: "repeat(24, 1fr)" }}>{Array(24).fill(0).map((_, index) => <p key={index} className="text-xs text-muted-foreground">{startAtTime(index, 6).toString().padStart(2, "0")}:00</p>)}</div>
}

/** All-day events of Apple calendars above the hours, on every day they last. */
const AllDayEvents = ({ columnDate }: { columnDate: Date }) => {
    const events = useEventStore((state) => state.events)
    const dayEnd = new Date(columnDate)
    dayEnd.setDate(dayEnd.getDate() + 1)

    return events.filter((e) => e.allDay && e.startDate < dayEnd && e.endDate > columnDate).map((e) =>
        <p key={e.id} title={`${e.title} · ${e.external?.calendarName ?? ""}`}
            className="w-full truncate rounded bg-muted px-2 py-0.5 text-xs text-muted-foreground">{e.title || "Ohne Titel"}</p>)
}

const Day = ({ columnDate }: { columnDate: Date }) => {
    const events = useEventStore((state) => state.events)

    const filteredEvents = events.filter((e) => !e.allDay &&
        e.startDate.getDate() === columnDate.getDate() &&
        e.startDate.getMonth() === columnDate.getMonth() &&
        e.startDate.getFullYear() === columnDate.getFullYear()
    )

    return <div className='w-full'>
        {Array(24).fill(0).map((_, hour) =>
            <TimeSlot events={filteredEvents} hour={startAtTime(hour, 6)} columnDate={columnDate} key={hour} />
        )}
    </div>
}

const TimeSlot = ({ hour, events, columnDate }: { hour: number, events: CalendarEvent[], columnDate: Date }) => {
    const [open, setOpen] = useState<boolean>(false)
    const startDate = new Date(columnDate.getTime())
    startDate.setHours(hour, 0, 0, 0)

    return <div style={{ height: `${HOUR_HEIGHT}px` }} className="w-full relative block bg-card text-sm text-card-foreground border hover:cursor-default">
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger className="block w-full h-full">
            </DialogTrigger>
            <DialogContent showCloseButton={false} className="gap-0 overflow-hidden bg-muted p-0 sm:max-w-md">
                <EventDialog calendarEvent={null} closeDialog={() => setOpen(false)} startDate={startDate} />
            </DialogContent>
        </Dialog>
        <div>
            {events.filter((e) => e.startDate.getHours() >= hour && e.startDate.getHours() < hour + 1).map((item) =>
                // a repeating event has the same id at every occurrence
                <Event event={item} key={`${item.id}-${item.startDate.getTime()}`} />
            )}
        </div>
    </div>
}
const Event = ({ event }: { event: CalendarEvent }) => {
    const [open, setOpen] = useState<boolean>(false)
    // duration in minutes, from the timestamps so the start minutes are taken off and not added
    const durationMinutes = (event.endDate.getTime() - event.startDate.getTime()) / 60000
    const startMinutes = event.startDate.getMinutes()
    const compact = durationMinutes < 45

    if (event.external) {
        // read only, it's changed in Apple Calendar; clicks go through to the time slot below
        return <div style={{ height: `${Math.max(durationMinutes, 15) * HOUR_HEIGHT / 60}px`, top: `${startMinutes * HOUR_HEIGHT / 60}px` }}
            className="pointer-events-none absolute w-full z-40 text-xs leading-tight">
            <div className='w-full h-full overflow-hidden rounded border-l-4 border-muted-foreground/40 bg-muted px-2 py-1 text-muted-foreground'>
                <h3 className='truncate'>{event.title || "Ohne Titel"}</h3>
                <p className='truncate text-[10px]'>{formatTime(event.startDate)} - {formatTime(event.endDate)} · {event.external.calendarName}</p>
            </div>
        </div>
    }

    return <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger style={{ height: `${durationMinutes * HOUR_HEIGHT / 60}px`, top: `${startMinutes * HOUR_HEIGHT / 60}px` }}
            className={`absolute w-full z-50 text-xs leading-tight text-left`}>
            <div className='w-full h-full overflow-hidden bg-blue-300 rounded px-2 py-1 text-white'>
                <div className='flex items-center gap-1'>
                    <h3 className='truncate'>{event.title}</h3>
                    {event.recurrence && <Repeat className="size-3 shrink-0" aria-label="Wiederholt sich" />}
                    {/* short events have no room for a second line */}
                    {compact && <span className='shrink-0 text-[10px]'>{formatTime(event.startDate)}</span>}
                    {/* everyone taking part, the creator first; in the title row so they aren't cut off */}
                    {event.invitees.length > 0 && <AvatarStack className="ml-auto shrink-0 -space-x-1" avatarClassName="size-4 text-[8px] ring-1"
                        users={[...(event.creator ? [event.creator] : []), ...event.invitees]} />}
                </div>
                {!compact && <p className='text-[10px]'>{formatTime(event.startDate)} - {formatTime(event.endDate)}</p>}
                {!compact && event.workoutName && <p className='flex items-center gap-1 text-[10px] truncate'><Dumbbell className="size-3 shrink-0" />{event.workoutName}</p>}
            </div>
        </DialogTrigger>
        <DialogContent showCloseButton={false} className="gap-0 overflow-hidden bg-muted p-0 sm:max-w-md">
            <EventDialog calendarEvent={event} closeDialog={() => setOpen(false)} startDate={null} />
        </DialogContent>
    </Dialog>



}
const formatTime = (date: Date) => `${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}`
