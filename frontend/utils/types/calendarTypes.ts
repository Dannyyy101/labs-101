/** What the calendar shows of a person. */
export interface CalendarUser {
    id: string
    name: string | null
    image: string | null
}

/** How often an event repeats, null when it doesn't. */
export type Recurrence = "DAILY" | "WEEKLY" | "MONTHLY"

export interface CalendarEvent {
    id: string,
    title: string
    startDate: Date
    endDate: Date
    /** The workout planned for this event, null for a plain event. */
    workoutId: number | null
    workoutName?: string | null
    creatorId: string
    creator?: CalendarUser
    invitees: CalendarUser[]
    /** A repeating event is listed once per occurrence, all with the same id and their own dates. */
    recurrence: Recurrence | null
    /** No occurrence starts after this, null repeats forever. */
    recurrenceEnd: Date | null
    /** Set for an event of an Apple calendar, it can't be changed here. */
    external?: { calendarId: number, calendarName: string }
    /** Only events of Apple calendars, from midnight of the first day to midnight after the last. */
    allDay?: boolean
}

export interface CalendarEventDto {
    id: string,
    title: string
    startDate: string
    endDate: string
    workoutId: number | null
    workoutName: string | null
    creatorId: string
    creator: CalendarUser
    invitees: CalendarUser[]
    recurrence: Recurrence | null
    recurrenceEnd: string | null
}

/**
 * An Apple calendar shown in the user's calendar, of a connected Apple ID or shared by link (then without account).
 * {@link error} says why it couldn't be loaded the last time.
 */
export interface ExternalCalendar {
    id: number
    name: string
    url: string
    error: string | null
    accountId: number | null
    appleId: string | null
}

export interface ExternalCalendarEventDto {
    calendarId: number
    calendarName: string
    uid: string | null
    title: string | null
    startDate: string
    endDate: string
    allDay: boolean
}
