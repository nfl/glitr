package com.nfl.dm.shield.graphql.relay.type

import com.nfl.dm.shield.graphql.GlitrBuilder
import com.nfl.dm.shield.graphql.data.query.QueryType
import com.nfl.dm.shield.graphql.domain.graph.annotation.ForwardPagingArguments
import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import spock.lang.Specification

import javax.validation.constraints.NotNull
import java.lang.annotation.Annotation
import java.lang.reflect.Field
import java.lang.reflect.Method

class PagingOutputTypeConverterTest extends Specification {

    def glitr = GlitrBuilder.newGlitrWithRelaySupport().withQueryRoot(new QueryType()).build()
    def typeRegistry = glitr.typeRegistry
    def relayHelper = glitr.relayHelper
    def PagingOutputTypeConverter pagingOutputTypeConverter = new PagingOutputTypeConverter().setRelayHelper(relayHelper).setTypeRegistry(typeRegistry)


    def "simple string"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("simpleString");
        method = MyTestClass.class.getDeclaredMethod("getSimpleString");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("simpleString").getAnnotation(ForwardPagingArguments.class);
        expected = "has to be of type ParametrizedType";
    }

    def "simple string with non supported annotation"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("simpleString");
        method = MyTestClass.class.getDeclaredMethod("getSimpleString");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("simpleString").getAnnotation(NotNull.class);
        expected = " must be ";
    }

    def "array of strings"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("arrayOfStrings");
        method = MyTestClass.class.getDeclaredMethod("getArrayOfStrings");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("arrayOfStrings").getAnnotation(ForwardPagingArguments.class);
        expected = "has to be of type ParametrizedType"
    }

    def "collection of strings"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("collOfStrings");
        method = MyTestClass.class.getDeclaredMethod("getCollOfStrings");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("collOfStrings").getAnnotation(ForwardPagingArguments.class);
        expected = GraphQLObjectType.class
    }

    def "list of strings"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("listOfStrings");
        method = MyTestClass.class.getDeclaredMethod("getListOfStrings");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("listOfStrings").getAnnotation(ForwardPagingArguments.class);
        expected = GraphQLObjectType.class
    }

    def "non null list of strings"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("nonNullListOfStrings");
        method = MyTestClass.class.getDeclaredMethod("getNonNullListOfStrings");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("nonNullListOfStrings").getAnnotation(ForwardPagingArguments.class);
        expected = GraphQLObjectType.class
    }

    public void testArgumentsOfCall(Field field, Method method, Class declaringClass, Annotation annotation, Object expected) {
        try {
            GraphQLOutputType outputType = pagingOutputTypeConverter.call(field, method, declaringClass, annotation);
            outputType.getClass() == expected
        } catch (IllegalArgumentException e) {
            // test the error message
            e.getMessage().contains((CharSequence) expected)
        }
    }

    public class MyTestClass {
        @ForwardPagingArguments
        public List<String> listOfStrings;
        @ForwardPagingArguments
        @GraphQLNonNull
        public List<String> nonNullListOfStrings;
        @ForwardPagingArguments
        public Collection<String> collOfStrings;
        @ForwardPagingArguments
        @NotNull
        @GraphQLNonNull
        public String simpleString;
        @ForwardPagingArguments
        public String[] arrayOfStrings;

        public Collection<String> getCollOfStrings() {
            return collOfStrings;
        }

        public List<String> getListOfStrings() {
            return listOfStrings;
        }

        public String getSimpleString() {
            return simpleString;
        }

        public String[] getArrayOfStrings() {
            return arrayOfStrings;
        }

        public List<String> getNonNullListOfStrings() {
            return nonNullListOfStrings
        }
    }
}
