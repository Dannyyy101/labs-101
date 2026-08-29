import { CalendarEvent, CalendarEventDto } from "@/utils/types/calendarTypes";

export function mapFromCalendarEventDto(dto: CalendarEventDto): CalendarEvent {
    return { id: dto.id, title: dto.title, startDate: new Date(dto.startDate), endDate: new Date(dto.endDate), exerciseIds: [] }
}