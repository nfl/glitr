package com.nfl.dm.shield.graphql.error;

import com.nfl.dm.shield.web.response.NFLError;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

public class NFLGraphQLValidationError extends NFLGraphQLError {

    protected List<NFLGraphQLError> errors;


    public NFLGraphQLValidationError() {
        super(HttpStatus.BAD_REQUEST);
    }

    public List<NFLGraphQLError> getErrors() {
        return errors;
    }

    public NFLGraphQLError setErrors(List<NFLError> errors) {
        this.errors = errors.stream()
                .map(nflError -> {
                    String message = nflError.getMessage();

                    if (StringUtils.isNotBlank(nflError.getField())) {
                        message = nflError.getField() + ": " + nflError.getMessage();
                    }

                    return new NFLGraphQLValidationError()
                            .setInput(nflError.getInput())
                            .setMessage(message);
                })
                .collect(Collectors.toList());
        return this;
    }
}
