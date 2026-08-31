package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.BodyPartDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.entities.BodyPart;
import com.labs_101.backend.entities.CalendarEvent;

public class BodyPartMapper {
    public static BodyPart fromDto(BodyPartDto dto) {
        return new BodyPart(null, dto.getSlug(), dto.getColor(), dto.getIntensity(), dto.getSide(), null);
    }
}
