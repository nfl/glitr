package com.nfl.glitr.util;

public class SerializationUtil {

    public static ObjectMapper objectMapper = new ObjectMapper() {

        private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        @Override
        public <T> T convertValue(Object fromValue, Class<T> toValueType) {
            return jacksonObjectMapper.convertValue(fromValue, toValueType);
        }
    };
}
