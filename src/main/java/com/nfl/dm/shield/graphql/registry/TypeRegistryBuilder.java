package com.nfl.dm.shield.graphql.registry;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLOutputType;
import rx.functions.Func4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeRegistryBuilder {
    private Map<Class, List<Object>> overrides = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, DataFetcher>> annotationToDataFetcherProviderMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap = new HashMap<>();
    private Relay relay = null;

    private TypeRegistryBuilder() {

    }

    public TypeRegistryBuilder withRelay(Relay relay) {
        this.relay = relay;
        return this;
    }

    public TypeRegistryBuilder withOverrides(Map<Class, List<Object>> overrides) {
        this.overrides = overrides;
        return this;
    }

    public TypeRegistryBuilder withAnnotationToDataFetcherProviderMap(Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, DataFetcher>> annotationToDataFetcherProviderMap) {
        this.annotationToDataFetcherProviderMap = annotationToDataFetcherProviderMap;
        return this;
    }

    public TypeRegistryBuilder withAnnotationToArgumentsProviderMap(Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap) {
        this.annotationToArgumentsProviderMap = annotationToArgumentsProviderMap;
        return this;
    }

    public TypeRegistryBuilder withAnnotationToGraphQLOutputTypeMap(Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap) {
        this.annotationToGraphQLOutputTypeMap = annotationToGraphQLOutputTypeMap;
        return this;
    }

    public TypeRegistryBuilder addCustomDataFetcherFunc(Class<? extends Annotation> annotationClass, Func4<Field, Method, Class, Annotation, DataFetcher> dataFetcherFunc4) {
        annotationToDataFetcherProviderMap.putIfAbsent(annotationClass, dataFetcherFunc4);
        return this;
    }

    public TypeRegistryBuilder addCustomFieldArgumentsFunc(Class<? extends Annotation> annotationClass, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> argumentsFunc4) {
        annotationToArgumentsProviderMap.putIfAbsent(annotationClass, argumentsFunc4);
        return this;
    }

    public TypeRegistryBuilder addCustomFieldOutputTypeFunc(Class<? extends Annotation> annotationClass, Func4<Field, Method, Class, Annotation, GraphQLOutputType> argumentsFunc4) {
        annotationToGraphQLOutputTypeMap.putIfAbsent(annotationClass, argumentsFunc4);
        return this;
    }

    public TypeRegistryBuilder addOverride(Class clazz, Object overrideObject) {
        overrides.putIfAbsent(clazz, new ArrayList<>());
        overrides.get(clazz).add(overrideObject);
        return this;
    }

    public static TypeRegistryBuilder newTypeRegistry() {
        return new TypeRegistryBuilder();
    }

    public TypeRegistry build() {
        return new TypeRegistry(overrides, annotationToDataFetcherProviderMap, annotationToArgumentsProviderMap, annotationToGraphQLOutputTypeMap, relay);
    }
}