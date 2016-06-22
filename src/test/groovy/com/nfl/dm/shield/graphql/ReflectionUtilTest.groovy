package com.nfl.dm.shield.graphql

import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLIgnore
import spock.lang.Specification

class ReflectionUtilTest extends Specification {

    def "test getMethodMap logic"() {
        expect:
        ReflectionUtil.getMethodMap(Video.class).keySet().contains(methodName) == eligible
        where:
        methodName                                      || eligible
        "getTitle"                                      || true
        "getId"                                         || true
        "isValid"                                       || true
        "nonGetterMethod"                               || false
        "getTitleWithGraphQLIgnoreOnField"              || false
        "getTitleWithGraphQLIgnoreOnGetter"             || false
        "getTitleWithGraphQLIgnoreOnBothFieldAndGetter" || false
        "getMap"                                        || false
    }

    def "test eligibleMethod logic"() {
        expect:
        ReflectionUtil.eligibleMethod(Video.getMethod(methodName)) == eligible

        where:
        methodName                                      || eligible
        "getTitle"                                      || true
        "getId"                                         || true
        "isValid"                                       || true
        "nonGetterMethod"                               || false
        "getTitleWithGraphQLIgnoreOnField"              || false
        "getTitleWithGraphQLIgnoreOnGetter"             || false
        "getTitleWithGraphQLIgnoreOnBothFieldAndGetter" || false
        "getMap"                                        || false
    }

    class Video {
        private title;

        def getTitle() {
            return title
        }

        def getId() {
            return null
        }

        def "isValid"() {
            return null
        }

        def nonGetterMethod() {
            return null
        }

        @GraphQLIgnore
        private titleWithGraphQLIgnoreOnField;

        def getTitleWithGraphQLIgnoreOnField() {
            return title
        }

        private titleWithGraphQLIgnoreOnGetter;

        @GraphQLIgnore
        def getTitleWithGraphQLIgnoreOnGetter() {
            return title
        }

        @GraphQLIgnore
        private titleWithGraphQLIgnoreOnBothFieldAndGetter;

        @GraphQLIgnore
        def getTitleWithGraphQLIgnoreOnBothFieldAndGetter() {
            return title
        }

        def Map getMap() {
            return null
        }
    }
}
