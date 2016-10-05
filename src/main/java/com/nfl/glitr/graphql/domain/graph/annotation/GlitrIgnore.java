package com.nfl.glitr.graphql.domain.graph.annotation;

import java.lang.annotation.*;

/**
 * Designate a GraphQL result or input field that should not be exposed
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface GlitrIgnore {
}
