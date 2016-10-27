package com.nfl.glitr.util;

public interface ObjectMapper {

    <T> T convertValue(Object fromValue, Class<T> toValueType);
}