package com.labs_101.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BodyPartDto {
    private Long id;
    private String slug;
    private String color;
    private Long intensity;
    private String side;
}
