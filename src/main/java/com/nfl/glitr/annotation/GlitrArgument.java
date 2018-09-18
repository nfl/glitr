package com.nfl.glitr.annotation;

import java.lang.annotation.*;

/**
 * Identifies and defines the GraphQL arguments available for a given field
 *
 *         name: name of the GraphQL argument
 *         type: Java Class for the argument
 *     nullable: whether the GraphQL argument is optional
 * defaultValue: default value
 *  description: description
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface GlitrArgument {

    String name();
    Class type();

    int NULLABILITY_OPEN = 0;
    int NULLABILITY_NON_NULL = 1;
    int NULLABILITY_NON_BLANK = 2;
    int nullability() default NULLABILITY_OPEN;

    String DEFAULT_DESCRIPTION = "";
    String description() default DEFAULT_DESCRIPTION;

    String NO_DEFAULT_VALUE = "No Default Value";
    String defaultValue() default NO_DEFAULT_VALUE;
}
