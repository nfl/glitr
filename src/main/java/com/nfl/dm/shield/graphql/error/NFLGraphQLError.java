package com.nfl.dm.shield.graphql.error;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import graphql.ErrorType;
import graphql.language.SourceLocation;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.http.HttpStatus;

import java.util.List;

@SuppressWarnings({"unused", "SimplifiableIfStatement"})
@JsonPropertyOrder({"httpStatus", "message", "errorType", "errors", "locations", "input", "stackTrace", "cause"})
public abstract class NFLGraphQLError extends RuntimeException implements graphql.GraphQLError {

    private HttpStatus httpStatus;
    private String message;
    private Object input;
    private ErrorType errorType;
    private List<SourceLocation> locations;


    public NFLGraphQLError(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public NFLGraphQLError setMessage(String message) {
        this.message = message;
        return this;
    }

    public Object getInput() {
        return input;
    }

    public NFLGraphQLError setInput(Object input) {
        this.input = input == null ? null : input.toString();
        return this;
    }

    public NFLGraphQLError setStacktrace(StackTraceElement[] stackTraceElements) {
        stackTraceElements = stackTraceElements == null ? new StackTraceElement[]{} : stackTraceElements;
        super.setStackTrace(stackTraceElements);
        return this;
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
    }

    public NFLGraphQLError setErrorType(ErrorType errorType) {
        this.errorType = errorType;
        return this;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return locations;
    }

    public NFLGraphQLError setLocations(List<SourceLocation> locations) {
        this.locations = locations;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
                .append("code", httpStatus.value())
                .append("message", message)
                .append("input", input)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NFLGraphQLError that = (NFLGraphQLError) o;

        if (httpStatus != that.httpStatus) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (input != null ? !input.equals(that.input) : that.input != null) return false;
        if (errorType != that.errorType) return false;
        return locations != null ? locations.equals(that.locations) : that.locations == null;

    }

    @Override
    public int hashCode() {
        int result = httpStatus.value();
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (input != null ? input.hashCode() : 0);
        result = 31 * result + (errorType != null ? errorType.hashCode() : 0);
        result = 31 * result + (locations != null ? locations.hashCode() : 0);
        return result;
    }
}
