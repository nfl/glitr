package com.nfl.glitr.annotation;

import java.lang.annotation.*;

/**
 * <pre>
 * Identifies and defines the GraphQL arguments available for a given field
 *
 *  name: name of the GraphQL argument
 *         type: Java Class for the argument
 *     required: whether the GraphQL argument is optional
 *  nullability: flag that doesn't have an any glitr level support and is designed to be used on the client endpoint
 * defaultValue: default value
 *  description: description
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface GlitrArgument {

    String name();
    Class type();

    boolean required() default false;
    nullability nullability() default nullability.OPEN;

    String DEFAULT_DESCRIPTION = "";
    String description() default DEFAULT_DESCRIPTION;

    String NO_DEFAULT_VALUE = "No Default Value";
    String defaultValue() default NO_DEFAULT_VALUE;

    enum nullability {
        OPEN, NON_NULL, NON_BLANK
    }
}
