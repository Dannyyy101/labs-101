import { CalendarEvent } from "@/utils/types/calendarTypes"
import { create } from "zustand"


interface EventState {
    events: CalendarEvent[]
    setEvents: (events: CalendarEvent[]) => void
}

export const useEventStore = create <EventState>((set) => ({
    events: [],
    setEvents: ((events) => set(() => ({events: events})))
}))