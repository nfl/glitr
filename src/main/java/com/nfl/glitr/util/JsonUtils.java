package com.nfl.glitr.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class JsonUtils {

    private static ObjectMapper mapper = new ObjectMapper();


    static {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
        mapper.registerModule(new JavaTimeModule().addSerializer(new ZonedDateTimeSerializer(dateTimeFormatter)));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    public static JsonNode readTree(String content) {
        try {
            return mapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static  <T> T readValue(String content, Class<T> type) {
        try {
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeValueAsString(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to write value: " + o.toString(), e);
        }
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }
}
