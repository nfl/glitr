package com.nfl.dm.shield.graphql.registry.datafetcher;

import graphql.schema.DataFetcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface AnnotationBasedDataFetcherFactory {

    DataFetcher create(Field field, Method method, Class declaringClass);
}
