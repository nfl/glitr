package com.nfl.glitr.registry;

import com.nfl.glitr.registry.datafetcher.AnnotationBasedDataFetcherFactory;
import com.nfl.glitr.relay.Relay;
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
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap = new HashMap<>();
    private Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap = new HashMap<>();
    private Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap = new HashMap<>();

    private Relay relay = null;
    private boolean explicitRelayNodeScanEnabled = false;


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

    public TypeRegistryBuilder withAnnotationToArgumentsProviderMap(Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap) {
        this.annotationToArgumentsProviderMap = annotationToArgumentsProviderMap;
        return this;
    }

    public TypeRegistryBuilder withAnnotationToGraphQLOutputTypeMap(Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap) {
        this.annotationToGraphQLOutputTypeMap = annotationToGraphQLOutputTypeMap;
        return this;
    }

    public TypeRegistryBuilder withAnnotationToDataFetcherFactoryMap(Map<Class<? extends  Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap) {
        this.annotationToDataFetcherFactoryMap = annotationToDataFetcherFactoryMap;
        return this;
    }

    public TypeRegistryBuilder withAnnotationToDataFetcherMap(Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap) {
        this.annotationToDataFetcherMap = annotationToDataFetcherMap;
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

    public TypeRegistryBuilder withExplicitRelayNodeScan() {
        this.explicitRelayNodeScanEnabled = true;
        return this;
    }

    public static TypeRegistryBuilder newTypeRegistry() {
        return new TypeRegistryBuilder();
    }

    public TypeRegistry build() {
        return new TypeRegistry(overrides, annotationToDataFetcherFactoryMap, annotationToDataFetcherMap, annotationToArgumentsProviderMap, annotationToGraphQLOutputTypeMap, relay, explicitRelayNodeScanEnabled);
    }
}