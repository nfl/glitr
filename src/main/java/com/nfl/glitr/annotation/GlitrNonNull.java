package com.nfl.glitr.annotation;

import java.lang.annotation.*;

/**
 * Designate a GraphQL result or input field that can't be `null`
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface GlitrNonNull {
}
