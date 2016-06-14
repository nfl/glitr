package com.nfl.dm.shield.graphql.domain.graph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ForwardPagingArguments {
    String FIRST = "first";
    String AFTER = "after";
}
