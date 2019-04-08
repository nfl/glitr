package com.nfl.glitr.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface GlitrDescription {

    String DEFAULT_DESCRIPTION = "";
    String value() default DEFAULT_DESCRIPTION;
}
