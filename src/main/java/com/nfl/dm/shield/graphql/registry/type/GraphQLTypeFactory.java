package com.nfl.dm.shield.graphql.registry.type;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class GraphQLTypeFactory {

    private final Map<JavaType, DelegateTypeFactory> delegateFactories = new HashMap<>();
    private final Map<JavaType, DelegateTypeFactory> delegateInputFactories = new HashMap<>();


    /**
     * Add a {@link DelegateTypeFactory} (mapped by {@link JavaType} as the key) to delegateFactories
     *
     * @param factory {@link DelegateTypeFactory} that can create output types
     * @param types {@link JavaType}(s) to be resolved by given factory
     * @return {@link GraphQLTypeFactory}
     */
    public GraphQLTypeFactory withOutputTypeFactory(final DelegateTypeFactory factory, JavaType... types) {
        Map<JavaType, DelegateTypeFactory> factories = Arrays
                .stream(types)
                .collect(Collectors.toMap(type -> type, type -> factory));
        delegateFactories.putAll(factories);
        return this;
    }

    /**
     * Add a {@link DelegateTypeFactory} (mapped by {@link JavaType} as the key) to delegateInputFactories
     *
     * @param factory {@link DelegateTypeFactory} that can create input types
     * @param types {@link JavaType}(s) to be resolved by given factory
     * @return {@link GraphQLTypeFactory}
     */
    public GraphQLTypeFactory withInputTypeFactory(final DelegateTypeFactory factory, JavaType... types) {
        Map<JavaType, DelegateTypeFactory> factories = Arrays
                .stream(types)
                .collect(Collectors.toMap(type -> type, type -> factory));
        delegateInputFactories.putAll(factories);
        return this;
    }

    /**
     * Creates a {@link GraphQLOutputType} by inspecting the given a class
     *
     * @param clazz to be evaluated
     * @return {@link GraphQLOutputType}
     */
    public GraphQLOutputType createGraphQLOutputType(Class clazz) {
        JavaType javaType = getJavaTypeFromClass(clazz);
        return (GraphQLOutputType) delegateFactories.get(javaType).create(clazz);
    }

    /**
     * Creates a {@link GraphQLInputType} by inspecting the given a class
     *
     * @param clazz to be evaluated
     * @return {@link GraphQLInputType}}
     */
    public GraphQLInputType createGraphQLInputType(Class clazz) {
        JavaType javaType = getJavaTypeFromClass(clazz);
        if (javaType == JavaType.ENUM) {
            return (GraphQLInputType) delegateFactories.get(JavaType.ENUM).create(clazz);
        }
        return (GraphQLInputType) delegateInputFactories.get(javaType).create(clazz);
    }

    public static JavaType getJavaTypeFromClass(Class clazz) {
        JavaType javaType;
        if (clazz.isEnum()) {
            javaType = JavaType.ENUM;
        } else if (clazz.isInterface()) {
            javaType = JavaType.INTERFACE;
        } else if (Modifier.isAbstract(clazz.getModifiers())) {
            javaType = JavaType.ABSTRACT_CLASS;
        } else {
            javaType = JavaType.CLASS;
        }
        return javaType;
    }
}
