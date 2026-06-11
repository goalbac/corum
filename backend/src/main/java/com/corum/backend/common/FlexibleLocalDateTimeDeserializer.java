package com.corum.backend.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return parse(parser.getValueAsString());
    }

    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized);
        } catch (RuntimeException ignored) {
        }
        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (RuntimeException ignored) {
        }
        return ZonedDateTime.parse(normalized).toLocalDateTime();
    }
}
