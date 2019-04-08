package com.nfl.glitr.annotation;

import java.lang.annotation.*;

/**
 * Indicates that annotated selection is pageable
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GlitrForwardPagingArguments {

    String FIRST = "first";
    String AFTER = "after";
}
