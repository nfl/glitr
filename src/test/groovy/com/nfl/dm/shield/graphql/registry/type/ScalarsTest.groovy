package com.nfl.dm.shield.graphql.registry.type

import graphql.language.StringValue
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

public class ScalarsTest extends Specification {

    def "DateTime parse literal"() {
        expect:
        Scalars.GraphQLDateTime.getCoercing().parseLiteral(literal) == result

        where:
        literal                                     | result
        new StringValue("2016-01-08T00:32:09.132Z") | Instant.parse("2016-01-08T00:32:09.132Z")
        new StringValue("2016-01-08T00:32:09.132Z") | ZonedDateTime.parse("2016-01-08T00:32:09.132Z").toInstant()
        null                                        | null
    }

    def "DateTime serialize/parseValue object"() {
        expect:
        Scalars.GraphQLDateTime.getCoercing().serialize(value) == result
        Scalars.GraphQLDateTime.getCoercing().parseValue(value) == result

        where:
        value                                           | result
        Instant.parse("2016-01-08T00:32:09.132Z")       | "2016-01-08T00:32:09.132Z"
        Instant.ofEpochMilli(1454362550000l)            | "2016-02-01T21:35:50.000Z"
        ZonedDateTime.parse("2016-01-08T00:32:09.132Z") | "2016-01-08T00:32:09.132Z"
        null                                            | null
    }

    def "DateTime serialize/parseValue object unknown type"() {
        setup:
        def input = "Hello"
        when:
        Scalars.GraphQLDateTime.getCoercing().serialize(input)
        Scalars.GraphQLDateTime.getCoercing().parseValue(input)

        then:
        def e = thrown(IllegalArgumentException)
        e.getMessage() == "Can't serialize type class java.lang.String with value "+ input
    }


    def "Date parse literal"() {
        expect:
        Scalars.GraphQLDate.getCoercing().parseLiteral(literal) == result

        where:
        literal                       | result
        new StringValue("2016-01-08") | LocalDate.parse("2016-01-08")
        null                          | null
    }

    def "Date serialize/parseValue object"() {
        expect:
        Scalars.GraphQLDate.getCoercing().serialize(value) == result
        Scalars.GraphQLDate.getCoercing().parseValue(value) == result

        where:
        value                                           | result
        Instant.parse("2016-01-08T00:32:09.132Z")       | "2016-01-08"
        Instant.ofEpochMilli(1454362550000l)            | "2016-02-01"
        ZonedDateTime.parse("2016-01-08T00:32:09.132Z") | "2016-01-08"
        LocalDate.of(2015, 01, 01)                      | "2015-01-01"
        null                                            | null
    }

    def "Date serialize/parseValue object unknown type"() {
        setup:
        def input = "Hello"
        when:
        Scalars.GraphQLDate.getCoercing().serialize(input)
        Scalars.GraphQLDate.getCoercing().parseValue(input)

        then:
        def e = thrown(IllegalArgumentException)
        e.getMessage() == "Can't serialize type class java.lang.String with value "+ input
    }
}
