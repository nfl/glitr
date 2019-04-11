package com.nfl.glitr.registry.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.*;
import graphql.schema.Coercing;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class Scalars {

    private static final String UTC = "UTC";
    private static final String SHIELD_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final String SHIELD_LOCAL_DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(SHIELD_DATE_TIME_PATTERN).withZone(ZoneId.of(UTC));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(SHIELD_LOCAL_DATE_PATTERN).withZone(ZoneId.of(UTC));

    private static final ObjectMapper om = new ObjectMapper();

    /**
     * {@code GraphQLDateTime} represents a date time as `2014-08-20T18:00:00.000Z`
     * ISO-8601 compliant and in UTC
     */
    public static final GraphQLScalarType GraphQLDateTime = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("DateTime is represented in UTC with the following template: 2014-08-20T18:00:00.000Z")
            .coercing(new Coercing() {

                @Override
                public Object serialize(Object input) {
                    if (input instanceof ZonedDateTime) {
                        ZonedDateTime time = (ZonedDateTime) input;
                        return time.format(DATE_TIME_FORMATTER);
                    }

                    if (input instanceof Instant) {
                        Instant time = (Instant) input;
                        return DATE_TIME_FORMATTER.format(time);
                    }

                    if (input instanceof String) {
                        Object obj = formatAsInstant(input);
                        if (obj == null) {
                            obj = formatAsZonedDateTime(input);
                        }
                        if (obj == null) {
                            throw new CoercingSerializeException("Failed to parse/serialize GraphQLDateTime with value " + input.toString() + ". Value likely of an unsupported format.");
                        }
                        return serialize(obj);
                    }

                    throw new CoercingSerializeException("Can't serialize type " + input.getClass() + " with value " + input.toString());
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
                    Instant parse;
                    try {
                        parse = Instant.parse(encodedDateTime);
                    } catch (DateTimeParseException e) {
                        parse = LocalDate.parse(encodedDateTime, DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC);
                    }
                    return parse;
                }
            }).build();

    /**
     * {@code GraphQLDate} represents a date as `2014-08-20` without any timezone information.
     * This template can be used only for description of date but can't be represented as an instant on the time-line.
     * A birth date is the perfect use case.
     */
    public static final GraphQLScalarType GraphQLDate = GraphQLScalarType.newScalar()
            .name("Date")
            .description("Date represents a local date without any timezone information therefore should only be used for displaying purposes. The template is 2014-08-20")
            .coercing(new Coercing() {

                @Override
                public Object serialize(Object input) {
                    if (input instanceof LocalDate) {
                        LocalDate time = (LocalDate) input;
                        return time.format(DATE_FORMATTER);
                    }

                    if (input instanceof ZonedDateTime) {
                        ZonedDateTime time = (ZonedDateTime) input;
                        return time.format(DATE_FORMATTER);
                    }

                    if (input instanceof Instant) {
                        Instant time = (Instant) input;
                        return DATE_FORMATTER.format(time);
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
                            throw new CoercingSerializeException("Failed to parse/serialize GraphQLDate with value " + input.toString() + ". Value likely of an unsupported format.");
                        }
                        return serialize(obj);
                    }

                    throw new CoercingSerializeException("Can't serialize type " + input.getClass() + " with value " + input.toString());
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
            }).build();

    public static final GraphQLScalarType GraphQLMap = GraphQLScalarType.newScalar()
            .name("Map")
            .description("Object represented by map, where key is a property and value is a value of this property.")
            .coercing(new Coercing() {

                @Override
                public Object serialize(Object input) {
                    if (input == null) {
                        return null;
                    }

                    if (input instanceof Map) {
                        return input;
                    }

                    if (input instanceof String) {
                        try {
                            return om.readValue((String) input, Map.class);
                        } catch (IOException e) {
                            throw new CoercingSerializeException("Can't serialize type " + input.getClass() + " with value " + input.toString());
                        }
                    }

                    throw new CoercingSerializeException("Can't serialize type " + input.getClass() + " with value " + input.toString());
                }

                @Override
                public Object parseValue(Object input) {
                    return serialize(input);
                }

                /**
                 * Always parse to {@code Map}.
                 *
                 * @param input object string representation
                 * @return object as a {@code Map}
                 */
                @Override
                public Object parseLiteral(Object input) {
                    if (!(input instanceof ObjectValue)) return null;
                    ObjectValue obj = (ObjectValue) input;
                    Map<String, Object> map = new HashMap<>();
                    for (ObjectField of : obj.getObjectFields()) {
                        map.put(of.getName(), parseVariableValue(of.getValue()));
                    }
                    return map;
                }
            }).build();

    private static Object parseVariableValue(Value val) {
        if (val instanceof StringValue) {
            return ((StringValue) val).getValue();
        } else if (val instanceof IntValue) {
            return ((IntValue) val).getValue();
        } else if (val instanceof FloatValue) {
            return ((FloatValue) val).getValue();
        } else if (val instanceof BooleanValue) {
            return ((BooleanValue) val).isValue();
        } else if (val instanceof NullValue) {
            return null;
        } else if (val instanceof ObjectValue) {
            Map<String, Object> map = new HashMap<>();
            for (ObjectField of : ((ObjectValue) val).getObjectFields()) {
                map.put(of.getName(), parseVariableValue(of.getValue()));
            }
            return map;
        }

        throw new IllegalArgumentException("Can't serialize value " + val);
    }

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
            Instant parse;
            try {
                parse = Instant.parse(time);
            } catch (DateTimeParseException e) {
                parse = LocalDate.parse(time, DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC);
            }
            return parse;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
