package com.nfl.dm.shield.graphql.domain.graph.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface GlitrDescription {

    String DEFAULT_DESCRIPTION = "No Description";
    String value() default DEFAULT_DESCRIPTION;
}
