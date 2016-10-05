package com.nfl.glitr.graphql

import com.nfl.glitr.graphql.domain.graph.annotation.GlitrIgnore
import spock.lang.Specification

class ReflectionUtilTest extends Specification {

    @SuppressWarnings("GroovyPointlessBoolean")
    def "test getMethodMap logic"() {
        expect:
        ReflectionUtil.getMethodMap(Video.class).keySet().contains(methodName) == eligible
        where:
        methodName                                      || eligible
        "getTitle"                                      || true
        "getId"                                         || true
        "isValid"                                       || true
        "nonGetterMethod"                               || false
        "getTitleWithGlitrIgnoreOnField"                || false
        "getTitleWithGlitrIgnoreOnGetter"               || false
        "getTitleWithGlitrIgnoreOnBothFieldAndGetter"   || false
        "getMap"                                        || false
    }

    @SuppressWarnings("GroovyPointlessBoolean")
    def "test eligibleMethod logic"() {
        expect:
        ReflectionUtil.eligibleMethod(Video.getMethod(methodName)) == eligible

        where:
        methodName                                      || eligible
        "getTitle"                                      || true
        "getId"                                         || true
        "isValid"                                       || true
        "nonGetterMethod"                               || false
        "getTitleWithGlitrIgnoreOnField"                || false
        "getTitleWithGlitrIgnoreOnGetter"               || false
        "getTitleWithGlitrIgnoreOnBothFieldAndGetter"   || false
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

        @GlitrIgnore
        private titleWithGlitrIgnoreOnField;

        def getTitleWithGlitrIgnoreOnField() {
            return title
        }

        private titleWithGlitrIgnoreOnGetter;

        @GlitrIgnore
        def getTitleWithGlitrIgnoreOnGetter() {
            return title
        }

        @GlitrIgnore
        private titleWithGlitrIgnoreOnBothFieldAndGetter;

        @GlitrIgnore
        def getTitleWithGlitrIgnoreOnBothFieldAndGetter() {
            return title
        }

        def Map getMap() {
            return null
        }
    }
}
