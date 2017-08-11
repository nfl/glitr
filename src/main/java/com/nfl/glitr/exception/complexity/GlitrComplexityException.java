package com.nfl.glitr.exception.complexity;

import graphql.ErrorType;
import graphql.language.SourceLocation;

import java.util.List;

public abstract class GlitrComplexityException extends RuntimeException implements graphql.GraphQLError {

    // GraphQL Spec
    private ErrorType errorType;
    private List<SourceLocation> locations;

    public GlitrComplexityException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public GlitrComplexityException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public GlitrComplexityException() {
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return locations;
    }

    public GlitrComplexityException setLocations(List<SourceLocation> locations) {
        this.locations = locations;
        return this;
    }

}