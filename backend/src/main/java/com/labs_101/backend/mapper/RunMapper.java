package com.labs_101.backend.mapper;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.CreateRunDto;
import com.labs_101.backend.entities.Run;

@Service
public class RunMapper {
    public static Run fromCreateRunDto(CreateRunDto dto){
        return new Run(dto.uuid(), dto.startDate(), dto.endDate(), dto.duration());
    }
}
