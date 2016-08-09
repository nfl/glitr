package com.nfl.dm.shield.graphql.exception;

public class GlitrException extends RuntimeException {

    public GlitrException(String message) {
        super(message);
    }

    public GlitrException(String message, Throwable cause) {
        super(message, cause);
    }
}
