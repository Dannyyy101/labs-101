import { Training } from "@/components/calendar/calendar"
import { WorkoutTemplate } from "./workoutTypes"

export interface CalendarEvent {
    id: string,
    title: string
    startDate: Date
    endDate: Date
    training?: Training | WorkoutTemplate
}

export interface CalendarEventDto {
    id: string,
    title: string
    startDate: string
    endDate: string
    training?: Training | WorkoutTemplate
}