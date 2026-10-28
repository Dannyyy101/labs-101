package com.labs_101.backend.mcp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

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

    int integer(String name, int fallback, int minimum, int maximum) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || value.isNull()) {
            return fallback;
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
