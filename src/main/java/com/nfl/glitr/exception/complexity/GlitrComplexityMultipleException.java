package com.nfl.glitr.exception.complexity;

import java.util.List;

public class GlitrComplexityMultipleException extends GlitrComplexityException {

    private List<GlitrComplexityException> exceptions;

    public GlitrComplexityMultipleException(List<GlitrComplexityException> exceptions) {
        super();
        this.exceptions = exceptions;
    }

    public List<GlitrComplexityException> getExceptions() {
        return exceptions;
    }

}
