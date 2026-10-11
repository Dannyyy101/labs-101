import { CalendarEvent } from "@/utils/types/calendarTypes"
import { create } from "zustand"


interface EventState {
    events: CalendarEvent[]
    setEvents: (events: CalendarEvent[]) => void
    /** Changes when the shown events have to be fetched again, the occurrences of a repeating event come from the backend. */
    version: number
    reload: () => void
}

export const useEventStore = create <EventState>((set) => ({
    events: [],
    setEvents: ((events) => set(() => ({events: events}))),
    version: 0,
    reload: () => set((state) => ({ version: state.version + 1 })),
}))
