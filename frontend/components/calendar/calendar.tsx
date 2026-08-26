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


export interface Training {
    id: string
    name: string,
    description: string
    duration: number
}

export default function Calendar() {
    const currentDate = new Date();
    currentDate.setDate(currentDate.getDate() - currentDate.getDay())
    const [dateRange, setDateRange] = useState<Date>(currentDate)

    const endDate = new Date()
    endDate.setHours(endDate.getHours() + 1, 30)

    const setEvents = useEventStore((state) => state.setEvents)

    const increaseDateByDays = (date: Date, days: number) => {
        const newDate = new Date(date)
        newDate.setDate(date.getDate() + days)
        return newDate
    }

    useEffect(() => {
        const fetch = async () => {
            setEvents((await getAllCalendarEvents()).value || [])
        }
        fetch()
    }, [])


    return (
        <div className="w-10/12 h-10/12 mt-20 relative">
            <div className="absolute -top-10 right-0 z-20 flex">
                <Button variant="secondary" onClick={() => setDateRange((prev) => increaseDateByDays(prev, -7))}><ChevronLeft /></Button>
                <Button variant="secondary" onClick={() => setDateRange((prev) => increaseDateByDays(prev, 7))}><ChevronRight /></Button>
            </div>

            <div className="grid h-10" style={{ gridTemplateColumns: "60px repeat(7, 1fr)" }}>
                <div></div>
                {Array(7).fill(0).map((_, index) => (
                    <div key={index} className="sticky top-0 z-10 bg-white flex items-center justify-center">
                        <p className="text-center">{increaseDateByDays(dateRange, index).toDateString()}</p>
                    </div>
                ))}
            </div>
            <div className="grid overflow-y-auto max-h-200" style={{ gridTemplateColumns: "60px repeat(7, 1fr)" }}>
                <Time />

                {Array(7).fill(0).map((_, index) => (
                    <Day day={index} currentDate={currentDate} key={index} />
                ))}
            </div>
        </div>
    );
}


const Time = () => {
    return <div className="grid" style={{ gridTemplateRows: "repeat(24, 1fr)" }}>{Array(24).fill(0).map((_, index) => <p key={index}>{index.toString().padStart(2, "0")}:00</p>)}</div>
}

const Day = ({ day, currentDate }: { day: number, currentDate: Date }) => {
    const events = useEventStore((state) => state.events)
    const filteredEvents = events.filter((e) => e.startDate.getDay() === day)
    return <div className='w-full'>
        {Array(24).fill(0).map((_, hour) => <TimeSlot events={filteredEvents} hour={hour} day={day} key={hour} currentDate={currentDate} />)}
    </div>
}

const TimeSlot = ({ hour, day, events, currentDate }: { hour: number, day: number, events: CalendarEvent[], currentDate: Date }) => {
    const [open, setOpen] = useState<boolean>(false)
    const startDate = new Date(currentDate.getTime())
    startDate.setDate(startDate.getDate() + day)
    startDate.setHours(hour)
    startDate.setMinutes(0)
    startDate.setSeconds(0)


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
                <Event event={item} key={hour} />
            )}
        </div>
    </div>
}
const Event = ({ event }: { event: CalendarEvent }) => {
    const [open, setOpen] = useState<boolean>(false)
    const timeDif = (event.endDate.getHours() - event.startDate.getHours())
    const hourDistance = event.startDate.getMinutes()
    const minutesDistance = event.startDate.getMinutes() + event.endDate.getMinutes()

    const isSameDay = event.startDate.getDay() === event.endDate.getDay()

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