import { CalendarEvent, CalendarEventDto } from "@/utils/types/calendarTypes";

export function mapFromCalendarEventDto(dto: CalendarEventDto): CalendarEvent {
    return { id: dto.id, title: dto.title, startDate: new Date(dto.startDate), endDate: new Date(dto.endDate), workoutId: dto.workoutId, workoutName: dto.workoutName, creatorId: dto.creatorId, creator: dto.creator, invitees: dto.invitees ?? [] }
}
