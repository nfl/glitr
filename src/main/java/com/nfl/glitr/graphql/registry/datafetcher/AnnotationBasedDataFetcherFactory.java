package com.nfl.glitr.graphql.registry.datafetcher;

import graphql.schema.DataFetcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface AnnotationBasedDataFetcherFactory {

    DataFetcher create(@Nullable Field field, @Nonnull Method method, @Nonnull Class declaringClass, @Nonnull Annotation annotation);
}
