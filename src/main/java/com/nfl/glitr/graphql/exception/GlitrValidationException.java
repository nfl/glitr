package com.nfl.glitr.graphql.exception;

import javax.validation.ConstraintViolation;
import java.util.Set;

public class GlitrValidationException extends GlitrException {

    private final Set<ConstraintViolation<Object>> violations;


    public GlitrValidationException(String message, Set<ConstraintViolation<Object>> violations) {
        super(message);
        this.violations = violations;
    }

    public Set<ConstraintViolation<Object>> getViolations() {
        return violations;
    }
}
