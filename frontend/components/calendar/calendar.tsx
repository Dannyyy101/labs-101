'use client'
import {
    Dialog,
    DialogContent, DialogTrigger
} from "@/components/ui/dialog";
import { useEventStore } from '@/lib/zustand/eventStore';
import { useEffect, useState } from 'react';

import { EventDialog } from './EventDialog';
import { CalendarEvent } from '@/utils/types/calendarTypes';
import { getAllCalendarEvents } from './action';
import { Button } from "../ui/button";
import { ChevronRight, ChevronLeft } from "lucide-react";
import { startAtTime } from "@/utils/date";


export interface Training {
    id: string
    name: string,
    description: string
    duration: number
}

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

    useEffect(() => {
        if (!isSizeKnown) return

        const fetch = async () => {
            setEvents((await getAllCalendarEvents(dateRange, STEPS)).value || [])
        }

        fetch()
    }, [dateRange, STEPS, isSizeKnown])

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

            <div className="mt-10 grid h-10" style={{ gridTemplateColumns: `60px repeat(${STEPS}, minmax(0, 1fr))` }}>
                <div></div>
                {Array(STEPS).fill(0).map((_, index) => (
                    <div key={index} className="sticky top-0 z-10 flex items-center justify-center">
                        <p className="text-center whitespace-nowrap truncate">{formatColumnDate(increaseDateByDays(dateRange, index))}</p>
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
    return <div className="grid" style={{ gridTemplateRows: "repeat(24, 1fr)" }}>{Array(24).fill(0).map((_, index) => <p key={index}>{startAtTime(index, 6).toString().padStart(2, "0")}:00</p>)}</div>
}

const Day = ({ columnDate }: { columnDate: Date }) => {
    const events = useEventStore((state) => state.events)

    const filteredEvents = events.filter((e) =>
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

    return <div className="w-full relative block bg-card text-sm text-card-foreground border hover:cursor-default">
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger className="w-full h-20">
            </DialogTrigger>
            <DialogContent className="sm:max-w-[80vw]">
                <EventDialog calendarEvent={null} closeDialog={() => setOpen(false)} startDate={startDate} />
            </DialogContent>
        </Dialog>
        <div>
            {events.filter((e) => e.startDate.getHours() >= hour && e.startDate.getHours() < hour + 1).map((item) =>
                <Event event={item} key={item.id} />
            )}
        </div>
    </div>
}
const Event = ({ event }: { event: CalendarEvent }) => {
    const [open, setOpen] = useState<boolean>(false)
    const timeDif = (event.endDate.getHours() - event.startDate.getHours())
    const hourDistance = event.startDate.getMinutes()
    const minutesDistance = event.startDate.getMinutes() + event.endDate.getMinutes()

    return <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger style={{ height: `${87 * timeDif + minutesDistance * 87 / 60}px`, top: `${hourDistance * 87 / 60}px` }}
            className={`absolute w-full z-50 text-sm`}>
            <div className='w-full h-full bg-blue-300 rounded p-2'>
                <h3 className='text-white '>{event.title}</h3>
                <p className='text-white text-xs'>{event.startDate.getHours().toString().padStart(2, "0")}
                    :
                    {event.startDate.getMinutes().toString().padStart(2, "0")}
                    -
                    {event.endDate.getHours().toString().padStart(2, "0")}
                    :
                    {event.endDate.getMinutes().toString().padStart(2, "0")}
                </p>
            </div>
        </DialogTrigger>
        <DialogContent className="sm:max-w-[80vw]">
            <EventDialog calendarEvent={event} closeDialog={() => setOpen(false)} startDate={null} />
        </DialogContent>
    </Dialog>



}