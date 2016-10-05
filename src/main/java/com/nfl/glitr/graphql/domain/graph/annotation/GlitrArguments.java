package com.nfl.glitr.graphql.domain.graph.annotation;

import java.lang.annotation.*;

/**
 * Container for the {@link GlitrArgument} annotation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface GlitrArguments {

    GlitrArgument[] value();
}
