package com.nfl.glitr.exception;

import org.slf4j.event.Level;

public class GlitrOverrideException extends RuntimeException {

    private Level logLevel = Level.ERROR;


    public GlitrOverrideException(Throwable cause) {
        super(cause);
    }

    public GlitrOverrideException(Throwable cause, Level logLevel) {
        super(cause);
        this.logLevel = logLevel;
    }

    public Level getLogLevel() {
        return logLevel;
    }
}
