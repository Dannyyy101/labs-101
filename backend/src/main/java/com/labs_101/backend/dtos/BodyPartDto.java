package com.labs_101.backend.dtos;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BodyPartDto {
    private @Nullable Long id;
    private String slug;
    private @Nullable String color;
    private @Nullable Long intensity;
    private @Nullable String side;
}
