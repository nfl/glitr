package com.nfl.glitr.relay.type

import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.query.QueryType
import com.nfl.glitr.annotation.GlitrForwardPagingArguments
import com.nfl.glitr.annotation.GlitrNonNull
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
        annotation = MyTestClass.class.getField("simpleString").getAnnotation(GlitrForwardPagingArguments.class);
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
        annotation = MyTestClass.class.getField("arrayOfStrings").getAnnotation(GlitrForwardPagingArguments.class);
        expected = "has to be of type ParametrizedType"
    }

    def "collection of strings"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("collOfStrings");
        method = MyTestClass.class.getDeclaredMethod("getCollOfStrings");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("collOfStrings").getAnnotation(GlitrForwardPagingArguments.class);
        expected = GraphQLObjectType.class
    }

    def "list of strings"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("listOfStrings");
        method = MyTestClass.class.getDeclaredMethod("getListOfStrings");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("listOfStrings").getAnnotation(GlitrForwardPagingArguments.class);
        expected = GraphQLObjectType.class
    }

    def "non null list of strings"() throws NoSuchFieldException, NoSuchMethodException {
        expect:
        testArgumentsOfCall(field, method, declaringClass, annotation, expected)

        where:
        field = MyTestClass.class.getField("nonNullListOfStrings");
        method = MyTestClass.class.getDeclaredMethod("getNonNullListOfStrings");
        declaringClass = MyTestClass.class;
        annotation = MyTestClass.class.getField("nonNullListOfStrings").getAnnotation(GlitrForwardPagingArguments.class);
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
        @GlitrForwardPagingArguments
        public List<String> listOfStrings;
        @GlitrForwardPagingArguments
        @GlitrNonNull
        public List<String> nonNullListOfStrings;
        @GlitrForwardPagingArguments
        public Collection<String> collOfStrings;
        @GlitrForwardPagingArguments
        @NotNull
        @GlitrNonNull
        public String simpleString;
        @GlitrForwardPagingArguments
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
