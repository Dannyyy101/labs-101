/** What the calendar shows of a person. */
export interface CalendarUser {
    id: string
    name: string | null
    image: string | null
}

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
}
