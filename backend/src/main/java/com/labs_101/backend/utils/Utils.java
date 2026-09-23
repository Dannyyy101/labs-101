package com.labs_101.backend.utils;

import java.util.function.Consumer;

public class Utils {
    public static <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null)
            setter.accept(value);
    }
}
