package com.nfl.dm.shield.graphql.exception;

import com.nfl.dm.shield.web.response.NFLError;

import java.util.List;

public class GlitrValidationException extends GlitrException {

    private final List<NFLError> violations;

    public GlitrValidationException(String message, List<NFLError> violations) {
        super(message);
        this.violations = violations;
    }

    public List<NFLError> getViolations() {
        return violations;
    }
}
