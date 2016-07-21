package com.nfl.dm.shield.graphql;

import com.nfl.dm.shield.graphql.domain.graph.annotation.*;
import graphql.execution.batched.Batched;
import graphql.execution.batched.BatchedDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReflectionUtil {

    private final static Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);


    /**
     * Determines if the given method is eligible for inclusion in the GraphQL Schema
     * @param method to inspect
     * @return true if the method name starts with `get` or `is` and false otherwise or if the method or the corresponding field is annotated with GraphQLIgnore
     */
    public static Boolean eligibleMethod(Method method) {
        Annotation graphQLIgnore = method.getAnnotation(GraphQLIgnore.class);
        if (graphQLIgnore != null) {
            return false;
        }

        String methodName = method.getName();
        String fieldName = sanitizeMethodName(methodName);

        Field field;
        try {
            field = method.getDeclaringClass().getDeclaredField(fieldName);
            Annotation fieldAnnotations = field.getAnnotation(GraphQLIgnore.class);
            if (fieldAnnotations != null) {
                return false;
            }
        } catch (NoSuchFieldException e) {
            // there is no field existing for the given method
            logger.debug("Field not found: {}", e);
        }

        return (methodName.startsWith("is") || methodName.startsWith("get"))
                && method.getDeclaringClass() != Object.class
                && (!Map.class.isAssignableFrom(method.getReturnType()));
    }

    /**
     * Returns an alphabetically sorted Map of the method names to the actual Method and referencing class
     * @param clazz to inspect
     * @return method map
     */
    public static Map<String, Pair<Method, Class>> getMethodMap(Class clazz) {
        return new TreeMap<>(Arrays.stream(clazz.getMethods())
                .filter(ReflectionUtil::eligibleMethod)
                .collect(Collectors.toMap(Method::getName, y -> Pair.of(y, y.getDeclaringClass()))));
    }

    public static String sanitizeMethodName(String name) {
        return StringUtils.uncapitalize(
                name.startsWith("is")
                        ? name.substring(2)
                        : name.substring(3));
    }

    public static Type getActualTypeArgumentFromType(ParameterizedType parameterizedType) {
        Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();

        if (fieldArgTypes.length > 1) {
            throw new IllegalArgumentException("Type can only have one generic argument: " + parameterizedType);
        }

        return fieldArgTypes[0];
    }

    public static Argument[] getArgumentsFromAnnotations(Map<Class, Annotation> annotationMap) {
        Argument[] singleAnnotationArguments = annotationMap.containsKey(Argument.class) ? new Argument[] {(Argument)annotationMap.get(Argument.class)}  : new Argument[0];
        Argument[] groupedAnnotationArguments = annotationMap.containsKey(Arguments.class) ? ((Arguments)annotationMap.get(Arguments.class)).value() : new Argument[0];
        return ArrayUtils.addAll(singleAnnotationArguments, groupedAnnotationArguments);
    }

    public static Argument[] getArgumentsFromMethod(Method method) {
        Argument[] singleAnnotationArguments = method.isAnnotationPresent(Argument.class) ? method.getAnnotationsByType(Argument.class) : new Argument[0];
        Argument[] groupedAnnotationArguments = method.isAnnotationPresent(Arguments.class) ? method.getAnnotation(Arguments.class).value() : new Argument[0];
        return ArrayUtils.addAll(singleAnnotationArguments, groupedAnnotationArguments);
    }

    public static Argument[] getArgumentsFromField(Field field) {
        if (field == null) {
            return new Argument[0];
        }
        Argument[] singleAnnotationArguments = field.isAnnotationPresent(Argument.class) ? field.getAnnotationsByType(Argument.class) : new Argument[0];
        Argument[] groupedAnnotationArguments = field.isAnnotationPresent(Arguments.class) ? field.getAnnotation(Arguments.class).value() : new Argument[0];
        return ArrayUtils.addAll(singleAnnotationArguments, groupedAnnotationArguments);
    }

    /**
     * Tells whether a given data fetcher supports batching
     *  {code @Batched} on the get method or instance of {code BatchedDataFetcher}
     *
     * @param supplied data fetcher
     * @return true if batched, false otherwise
     */
    public static boolean isDataFetcherBatched(DataFetcher supplied) {
        if (supplied instanceof BatchedDataFetcher) {
            return true;
        }

        try {
            Method getMethod = supplied.getClass().getMethod("get", DataFetchingEnvironment.class);
            Batched batched = getMethod.getAnnotation(Batched.class);
            if (batched != null) {
                return true;
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        return false;
    }

    /**
     * Retrieves the GraphQL documentation description value if present on an element
     * @param element that may be annotated with the GraphQLDescription
     * @return GraphQLDescription.value if present or GraphQLDescription.DEFAULT_DESCRIPTION otherwise
     */
    public static String getDescriptionFromAnnotatedElement(AnnotatedElement element) {
        return element.isAnnotationPresent(GraphQLDescription.class) ? element.getAnnotation(GraphQLDescription.class).value() : GraphQLDescription.DEFAULT_DESCRIPTION;
    }

    public static boolean isAnnotatedElementNullable(AnnotatedElement element) {
        return !element.isAnnotationPresent(GraphQLNonNull.class);
    }
}
