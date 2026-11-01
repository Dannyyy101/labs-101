package com.labs_101.backend.mcp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * The arguments of a tool call, a wrong value ends the call with an
 * {@link IllegalArgumentException} whose message Claude gets to see.
 */
final class McpArguments {
    /** Days are counted in the zone of the user, like on the home page. */
    static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private final JsonNode node;

    McpArguments(JsonNode node) {
        this.node = node;
    }

    String string(String name) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw new IllegalArgumentException(name + " has to be a string");
        }
        String text = value.asString().strip();
        return text.isEmpty() ? null : text;
    }

    String requiredString(String name) {
        String value = string(name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /** A positive number, null when not set. */
    Double number(String name) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber() || !(value.asDouble() > 0)) {
            throw new IllegalArgumentException(name + " has to be a number above 0");
        }
        return value.asDouble();
    }

    /** A number of at least 0, the fallback when not set. */
    double nonNegative(String name, double fallback) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (!value.isNumber() || !(value.asDouble() >= 0)) {
            throw new IllegalArgumentException(name + " has to be a number of at least 0");
        }
        return value.asDouble();
    }

    /** An id, given as number or string, null when not set. */
    Long id(String name) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isIntegralNumber() && value.canConvertToLong()) {
            return value.asLong();
        }
        try {
            if (value.isString()) {
                return Long.parseLong(value.asString().strip());
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        throw new IllegalArgumentException(name + " has to be an id");
    }

    LocalDate date(String name, LocalDate fallback) {
        String value = string(name);
        if (value == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " has to be a date like 2026-10-28");
        }
    }

    /** A time of day in the zone of the user, like 2026-10-28T18:30. */
    Instant dateTime(String name) {
        String value = requiredString(name);
        try {
            return LocalDateTime.parse(value).atZone(ZONE).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name + " has to be a date and time like 2026-10-28T18:30");
        }
    }

    int integer(String name, int fallback, int minimum, int maximum) {
        Integer number = integer(name, minimum, maximum);
        return number == null ? fallback : number;
    }

    /** A whole number between minimum and maximum, null when not set. */
    Integer integer(String name, int minimum, int maximum) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.canConvertToInt() || !value.isIntegralNumber()) {
            throw new IllegalArgumentException(name + " has to be a whole number");
        }
        int number = value.asInt();
        if (number < minimum || number > maximum) {
            throw new IllegalArgumentException(name + " has to be between " + minimum + " and " + maximum);
        }
        return number;
    }

    /** A list of objects that has at least one entry. */
    List<McpArguments> objects(String name) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (!value.isArray() || value.isEmpty()) {
            throw new IllegalArgumentException(name + " has to be a list with at least one entry");
        }
        List<McpArguments> objects = new ArrayList<>();
        for (JsonNode entry : value) {
            if (!entry.isObject()) {
                throw new IllegalArgumentException("the entries of " + name + " have to be objects");
            }
            objects.add(new McpArguments(entry));
        }
        return objects;
    }

    /**
     * {@code from} and {@code to} as days (both included), {@code to} defaults
     * to today and {@code from} to {@code defaultDays} before it.
     */
    Range range(int defaultDays, int maxDays) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate to = date("to", today);
        LocalDate from = date("from", to.minusDays(defaultDays - 1L));
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from has to be before to");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > maxDays) {
            throw new IllegalArgumentException("at most " + maxDays + " days at once");
        }
        return new Range(from, to);
    }

    record Range(LocalDate from, LocalDate to) {
        Instant start() {
            return from.atStartOfDay(ZONE).toInstant();
        }

        /** The end of the last day. */
        Instant end() {
            return to.plusDays(1).atStartOfDay(ZONE).toInstant();
        }
    }
}
