package com.nfl.glitr.registry.type;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Scalars {

    private static final String UTC = "UTC";
    private static final String SHIELD_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final String SHIELD_LOCAL_DATE_PATTERN = "yyyy-MM-dd";


    /**
     * {@code GraphQLDateTime} represents a date time as `2014-08-20T18:00:00.000Z`
     * ISO-8601 compliant and in UTC
     */
    public static final GraphQLScalarType GraphQLDateTime = new GraphQLScalarType("DateTime", "DateTime is represented in UTC with the following template: 2014-08-20T18:00:00.000Z", new Coercing() {

        @Override
        public Object serialize(Object input) {
            if (input == null) {
                return null;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(SHIELD_DATE_TIME_PATTERN).withZone(ZoneId.of(UTC));

            if (input instanceof ZonedDateTime) {
                ZonedDateTime time = (ZonedDateTime)input;
                return time.format(formatter);
            }

            if (input instanceof Instant) {
                Instant time = (Instant)input;
                return formatter.format(time);
            }

            if (input instanceof String) {
                Object obj = formatAsInstant(input);
                if (obj == null) {
                    obj = formatAsZonedDateTime(input);
                }
                if (obj == null) {
                    throw new IllegalArgumentException("Failed to parse/serialize GraphQLDateTime with value "+input.toString()+". Value likely of an unsupported format.");
                }
                return serialize(obj);
            }

            throw new IllegalArgumentException("Can't serialize type "+input.getClass()+" with value "+ input.toString());
        }

        @Override
        public Object parseValue(Object input) {
            return serialize(input);
        }

        /**
         * Always parse to {@code Instant}.
         *
         * @param input date time string representation
         * @return date as an {@code Instant}
         */
        @Override
        public Object parseLiteral(Object input) {
            if (!(input instanceof StringValue)) return null;
            String encodedDateTime = ((StringValue) input).getValue();
            return Instant.parse(encodedDateTime);
        }
    });

    /**
     * {@code GraphQLDate} represents a date as `2014-08-20` without any timezone information.
     * This template can be used only for description of date but can't be represented as an instant on the time-line.
     * A birth date is the perfect use case.
     */
    public static final GraphQLScalarType GraphQLDate = new GraphQLScalarType("Date", "Date represents a local date without any timezone " +
            "information therefore should only be used for displaying purposes. The template is 2014-08-20", new Coercing() {

        @Override
        public Object serialize(Object input) {
            if (input == null) {
                return null;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(SHIELD_LOCAL_DATE_PATTERN).withZone(ZoneId.of(UTC));

            if (input instanceof LocalDate) {
                LocalDate time = (LocalDate)input;
                return time.format(formatter);
            }

            if (input instanceof ZonedDateTime) {
                ZonedDateTime time = (ZonedDateTime)input;
                return time.format(formatter);
            }

            if (input instanceof Instant) {
                Instant time = (Instant)input;
                return formatter.format(time);
            }

            if (input instanceof String) {
                Object obj = formatAsInstant(input);
                if (obj == null) {
                    obj = formatAsLocalDate(input);
                }
                if (obj == null) {
                    obj = formatAsZonedDateTime(input);
                }
                if (obj == null) {
                    throw new IllegalArgumentException("Failed to parse/serialize GraphQLDate with value "+input.toString()+". Value likely of an unsupported format.");
                }
                return serialize(obj);
            }

            throw new IllegalArgumentException("Can't serialize type "+input.getClass()+" with value "+input.toString());
        }

        @Override
        public Object parseValue(Object input) {
            return serialize(input);
        }

        /**
         * Always parse to {@code LocalDate}.
         *
         * @param input date time string representation
         * @return date as an {@code LocalDate}
         */
        @Override
        public Object parseLiteral(Object input) {
            if (!(input instanceof StringValue)) return null;
            String encodedDateTime = ((StringValue) input).getValue();
            return LocalDate.parse(encodedDateTime);
        }
    });

    private static Object formatAsLocalDate(Object input) {
        try {
            String time = (String) input;
            return LocalDate.parse(time);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Object formatAsZonedDateTime(Object input) {
        try {
            String time = (String) input;
            return ZonedDateTime.parse(time);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Object formatAsInstant(Object input) {
        try {
            String time = (String) input;
            return Instant.parse(time);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
