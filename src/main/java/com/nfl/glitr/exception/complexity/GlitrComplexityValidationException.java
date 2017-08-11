package com.nfl.glitr.exception.complexity;

import graphql.ErrorType;
import graphql.validation.ValidationErrorType;

public class GlitrComplexityValidationException extends GlitrComplexityException {

    private String field;
    private String input;
    private ValidationErrorType validationErrorType;

    public GlitrComplexityValidationException(String message, ValidationErrorType validationErrorType) {
        super(message, null, ErrorType.ValidationError);
        this.validationErrorType = validationErrorType;
    }

    public String getField() {
        return field;
    }

    public GlitrComplexityValidationException setField(String field) {
        this.field = field;
        return this;
    }

    public String getInput() {
        return input;
    }

    public GlitrComplexityValidationException setInput(String input) {
        this.input = input;
        return this;
    }

    public ValidationErrorType getValidationErrorType() {
        return validationErrorType;
    }

    public GlitrComplexityValidationException setValidationErrorType(ValidationErrorType validationErrorType) {
        this.validationErrorType = validationErrorType;
        return this;
    }
}
