import { CalendarEvent, CalendarEventDto, ExternalCalendarEventDto } from "@/utils/types/calendarTypes";

export function mapFromCalendarEventDto(dto: CalendarEventDto): CalendarEvent {
    return { id: dto.id, title: dto.title, startDate: new Date(dto.startDate), endDate: new Date(dto.endDate), workoutId: dto.workoutId, workoutName: dto.workoutName, creatorId: dto.creatorId, creator: dto.creator, invitees: dto.invitees ?? [], recurrence: dto.recurrence ?? null, recurrenceEnd: dto.recurrenceEnd ? new Date(dto.recurrenceEnd) : null }
}

export function mapFromExternalCalendarEventDto(dto: ExternalCalendarEventDto): CalendarEvent {
    return {
        // occurrences of a repeating event have the same uid, the start tells them apart
        id: `external-${dto.calendarId}-${dto.uid}-${dto.startDate}`, title: dto.title ?? "", startDate: new Date(dto.startDate), endDate: new Date(dto.endDate),
        workoutId: null, creatorId: "", invitees: [], recurrence: null, recurrenceEnd: null,
        external: { calendarId: dto.calendarId, calendarName: dto.calendarName }, allDay: dto.allDay,
    }
}
