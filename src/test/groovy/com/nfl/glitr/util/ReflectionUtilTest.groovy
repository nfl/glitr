package com.nfl.glitr.util

import com.nfl.glitr.annotation.GlitrDeprecated
import com.nfl.glitr.annotation.GlitrIgnore
import org.apache.commons.lang3.reflect.MethodUtils
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.lang.reflect.Method

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
        "getMap"                                        || true
        "nonGetterMethod"                               || false
        "getTitleWithGlitrIgnoreOnField"                || false
        "getTitleWithGlitrIgnoreOnGetter"               || false
        "getTitleWithGlitrIgnoreOnBothFieldAndGetter"   || false
    }

    @SuppressWarnings("GroovyPointlessBoolean")
    def "test eligibleMethod logic"() {
        expect:
        ReflectionUtil.eligibleMethod(Video.getMethod(methodName)) == eligible

        where:
        methodName                                    || eligible
        "getTitle"                                    || true
        "getId"                                       || true
        "isValid"                                     || true
        "getMap"                                      || true
        "nonGetterMethod"                             || false
        "getTitleWithGlitrIgnoreOnField"              || false
        "getTitleWithGlitrIgnoreOnGetter"             || false
        "getTitleWithGlitrIgnoreOnBothFieldAndGetter" || false
    }

    abstract class AbstractContentTest  {
        @GlitrDeprecated("Test @GlitrDeprecated. This field is being deprecated.")
        //default private scope
        String description
        @GlitrDeprecated("Test @GlitrDeprecated. This field is also being deprecated")
        public String caption
        public String longDescription

        String getDescription() {
            return description
        }

        String getCaption() {
            return caption
        }

        String getLongDescription() {
            return longDescription
        }
    }

    class Video extends AbstractContentTest{
        @GlitrDeprecated("Test @GlitrDeprecated. This field is private")
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

        @GlitrDeprecated("Test GlitrDeprecated annotation")
        public String longTitle

        String getLongTitle() {
            return longTitle
        }

        def Map getMap() {
            return null
        }
    }


    def "check sanitizeMethodName works properly, (can accept short method names)"() {
        expect:
        ReflectionUtil.sanitizeMethodName(methodName) == sanitized

        where:
        methodName            || sanitized
        "getId"               || "id"
        "isEnabled"           || "enabled"
        "getVIDEO"            || "vIDEO"
        "l"                   || "l"
        "or"                  || "or"
        "and"                 || "and"
        "aVeryLongMethodName" || "aVeryLongMethodName"
    }

    def "test getAnnotationOfMethodOrField for annotations declared in superclass/interfaces.  Annotations should be found since annotations declared in superclass are supported."() {
        setup :
            Method privateMethod = MethodUtils.getAccessibleMethod(Video.class, "getDescription", null)
            Method publicMethod = MethodUtils.getAccessibleMethod(Video.class, "getCaption", null)
            Method publicWithoutAnnotationMethod = MethodUtils.getAccessibleMethod(Video.class, "getLongDescription", null)

        when :
            Optional<Annotation> privateFieldAnnotation = ReflectionUtil.getAnnotationOfMethodOrField(Video.class, privateMethod, GlitrDeprecated.class)
            Optional<Annotation> publicFieldAnnotation = ReflectionUtil.getAnnotationOfMethodOrField(Video.class, publicMethod, GlitrDeprecated.class)
            Optional<Annotation> publicFieldWithoutAnnotation = ReflectionUtil.getAnnotationOfMethodOrField(Video.class, publicWithoutAnnotationMethod, GlitrDeprecated.class)

        then :
            privateMethod != null
            publicMethod != null
            publicWithoutAnnotationMethod != null

            privateFieldAnnotation.isPresent() == true
            publicFieldAnnotation.isPresent() == true
            publicFieldWithoutAnnotation.isPresent() == false
    }

    def "test getAnnotationOfMethodOrField for annotations declared in specified class. Annotations should be found."() {
        setup :
            Method privateMethod = MethodUtils.getAccessibleMethod(Video.class, "getTitle", null)
            Method publicMethod = MethodUtils.getAccessibleMethod(Video.class, "getLongTitle", null)

        when :
            Optional<Annotation> privateFieldAnnotation = ReflectionUtil.getAnnotationOfMethodOrField(Video.class, privateMethod, GlitrDeprecated.class)
            Optional<Annotation> publicFieldAnnotation = ReflectionUtil.getAnnotationOfMethodOrField(Video.class, publicMethod, GlitrDeprecated.class)

        then :
            privateMethod != null
            publicMethod != null

            privateFieldAnnotation.isPresent() == true
            publicFieldAnnotation.isPresent() == true
    }
}
