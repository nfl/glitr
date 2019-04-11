package com.nfl.glitr.registry.type

import graphql.language.BooleanValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.schema.CoercingSerializeException
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

public class ScalarsTest extends Specification {

    @Unroll
    def "DateTime parse literal"() {
        expect:
            Scalars.GraphQLDateTime.getCoercing().parseLiteral(literal) == result

        where:
            literal                                     | result
            new StringValue("2016-01-08T00:32:09.132Z") | Instant.parse("2016-01-08T00:32:09.132Z")
            new StringValue("2016-01-08T00:32:09.132Z") | ZonedDateTime.parse("2016-01-08T00:32:09.132Z").toInstant()
            new StringValue("2016-01-08")               | Instant.parse("2016-01-08T00:00:00.000Z")
    }

    @Unroll
    def "DateTime serialize/parseValue object"() {
        expect:
            Scalars.GraphQLDateTime.getCoercing().serialize(value) == result
            Scalars.GraphQLDateTime.getCoercing().parseValue(value) == result

        where:
            value                                           | result
            Instant.parse("2016-01-08T00:32:09.132Z")       | "2016-01-08T00:32:09.132Z"
            Instant.ofEpochMilli(1454362550000l)            | "2016-02-01T21:35:50.000Z"
            ZonedDateTime.parse("2016-01-08T00:32:09.132Z") | "2016-01-08T00:32:09.132Z"
            "2016-01-08T00:32:09.132Z"                      | "2016-01-08T00:32:09.132Z"
            "2016-01-08"                                    | "2016-01-08T00:00:00.000Z"
    }

    def "DateTime serialize/parseValue object unknown type"() {
        setup:
            def input = "Hello"
        when:
            Scalars.GraphQLDateTime.getCoercing().serialize(input)
            Scalars.GraphQLDateTime.getCoercing().parseValue(input)

        then:
            def e = thrown(CoercingSerializeException)
            e.getMessage() == "Failed to parse/serialize GraphQLDateTime with value " + input + ". Value likely of an unsupported format."
    }

    @Unroll
    def "Date parse literal"() {
        expect:
            Scalars.GraphQLDate.getCoercing().parseLiteral(literal) == result

        where:
            literal                       | result
            new StringValue("2016-01-08") | LocalDate.parse("2016-01-08")
    }

    @Unroll
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
            "2016-01-08T00:32:09.132Z"                      | "2016-01-08"
            "2016-01-08"                                    | "2016-01-08"
    }

    def "Date serialize/parseValue object unknown type"() {
        setup:
            def input = "Hello"
        when:
            Scalars.GraphQLDate.getCoercing().serialize(input)
            Scalars.GraphQLDate.getCoercing().parseValue(input)

        then:
            def e = thrown(CoercingSerializeException)
            e.getMessage() == "Failed to parse/serialize GraphQLDate with value " + input + ". Value likely of an unsupported format."
    }

    @Unroll
    def "Map parse literal"() {
        expect:
            Scalars.GraphQLMap.getCoercing().parseLiteral(literal) == result

        where:
            literal                                                                                                                                | result
            new ObjectValue([new ObjectField("test1", new StringValue("test1")), new ObjectField("test2", new StringValue("test2"))])              | [test1: "test1", test2: "test2"]
            new ObjectValue([new ObjectField("test1", new IntValue(BigInteger.ZERO)), new ObjectField("test2", new IntValue(BigInteger.ONE))])     | [test1: 0, test2: 1]
            new ObjectValue([new ObjectField("test1", new IntValue(BigInteger.ZERO)), new ObjectField("test2", new IntValue(BigInteger.ONE))])     | [test1: 0, test2: 1]
            new ObjectValue([new ObjectField("test1", new FloatValue(BigDecimal.ZERO)), new ObjectField("test2", new FloatValue(BigDecimal.ONE))]) | [test1: 0F, test2: 1F]
            new ObjectValue([new ObjectField("test1", new BooleanValue(true)), new ObjectField("test2", new BooleanValue(false))])                 | [test1: true, test2: false]
            new ObjectValue([new ObjectField("test1", NullValue.Null), new ObjectField("test2", NullValue.Null)])                                  | [test1: null, test2: null]
    }

    @Unroll
    def "Map serialize/parseValue object"() {
        expect:
            Scalars.GraphQLMap.getCoercing().serialize(value) == result
            Scalars.GraphQLMap.getCoercing().parseValue(value) == result

        where:
            value                                  | result
            [test1: "test1", test2: "test2"]       | [test1: "test1", test2: "test2"]
            '{"test1": "test1", "test2": "test2"}' | [test1: "test1", test2: "test2"]
    }

    def "Map serialize/parseValue object unknown type"() {
        setup:
            def input = "Hello"
        when:
            Scalars.GraphQLMap.getCoercing().serialize(input)
            Scalars.GraphQLMap.getCoercing().parseValue(input)

        then:
            def e = thrown(CoercingSerializeException)
            e.getMessage() == "Can't serialize type class java.lang.String with value $input"
    }
}
