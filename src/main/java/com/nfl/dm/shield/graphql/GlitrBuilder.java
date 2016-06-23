package com.nfl.dm.shield.graphql;

import com.nfl.dm.shield.graphql.domain.graph.annotation.ForwardPagingArguments;
import com.nfl.dm.shield.graphql.registry.RelayNode;
import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import com.nfl.dm.shield.graphql.registry.TypeRegistryBuilder;
import com.nfl.dm.shield.graphql.registry.datafetcher.AnnotationBasedDataFetcherFactory;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.NodeFetcherService;
import com.nfl.dm.shield.graphql.relay.RelayHelper;
import com.nfl.dm.shield.graphql.relay.type.CustomFieldArgumentsFunc;
import com.nfl.dm.shield.graphql.relay.type.PagingOutputTypeConverter;
import com.nfl.dm.shield.graphql.relay.type.RelayNodeOutputTypeFunc;
import graphql.relay.Relay;
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

public class GlitrBuilder {

    private NodeFetcherService nodeFetcherService;
    private Map<Class, List<Object>> overrides = new HashMap<>();
    private Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap = new HashMap<>();
    private Relay relay = null;

    private GlitrBuilder() {
    }

    public GlitrBuilder withRelay(Relay relay) {
        this.relay = relay;
        return this;
    }

    public GlitrBuilder withNodeFetcherService(NodeFetcherService nodeFetcherService) {
        this.nodeFetcherService = nodeFetcherService;
        return this;
    }

    public GlitrBuilder withOverrides(Map<Class, List<Object>> overrides) {
        this.overrides = overrides;
        return this;
    }

    public GlitrBuilder withAnnotationToArgumentsProviderMap(Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap) {
        this.annotationToArgumentsProviderMap = annotationToArgumentsProviderMap;
        return this;
    }

    public GlitrBuilder withAnnotationToGraphQLOutputTypeMap(Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap) {
        this.annotationToGraphQLOutputTypeMap = annotationToGraphQLOutputTypeMap;
        return this;
    }

    public GlitrBuilder addCustomDataFetcherFactory(Class<? extends Annotation> annotationClass, AnnotationBasedDataFetcherFactory annotationBasedDataFetcherFactory) {
        annotationToDataFetcherFactoryMap.putIfAbsent(annotationClass, annotationBasedDataFetcherFactory);
        return this;
    }

    public GlitrBuilder addCustomFieldArgumentsFunc(Class<? extends Annotation> annotationClass, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> argumentsFunc4) {
        annotationToArgumentsProviderMap.putIfAbsent(annotationClass, argumentsFunc4);
        return this;
    }

    public GlitrBuilder addCustomFieldOutputTypeFunc(Class<? extends Annotation> annotationClass, Func4<Field, Method, Class, Annotation, GraphQLOutputType> argumentsFunc4) {
        annotationToGraphQLOutputTypeMap.putIfAbsent(annotationClass, argumentsFunc4);
        return this;
    }

    public GlitrBuilder addOverride(Class clazz, Object overrideObject) {
        overrides.putIfAbsent(clazz, new ArrayList<>());
        overrides.get(clazz).add(overrideObject);
        return this;
    }

    public static GlitrBuilder newGlitr() {
        return new GlitrBuilder();
    }

    public static GlitrBuilder newGlitrWithRelaySupport() {
        return new GlitrBuilder().withRelay(new Relay());
    }

    public Glitr build() {
        if (relay != null) {
            return buildGlitrWithRelaySupport();
        }
        return buildGlitr();
    }

    private Glitr buildGlitr() {
        TypeRegistry typeRegistry = TypeRegistryBuilder.newTypeRegistry()
                .withAnnotationToArgumentsProviderMap(annotationToArgumentsProviderMap)
                .withAnnotationToGraphQLOutputTypeMap(annotationToGraphQLOutputTypeMap)
                .withAnnotationToDataFetcherFactoryMap(annotationToDataFetcherFactoryMap)
                .withOverrides(overrides)
                .build();
        return new Glitr(typeRegistry, null);
    }

    private Glitr buildGlitrWithRelaySupport() {
        PagingOutputTypeConverter pagingOutputTypeConverter = new PagingOutputTypeConverter();
        RelayNodeOutputTypeFunc relayNodeOutputTypeFunc = new RelayNodeOutputTypeFunc();
        CustomFieldArgumentsFunc customFieldArgumentsFunc = new CustomFieldArgumentsFunc();

        // instantiate TypeRegistry
        TypeRegistry typeRegistry = TypeRegistryBuilder.newTypeRegistry()
                .withAnnotationToArgumentsProviderMap(annotationToArgumentsProviderMap)
                .withAnnotationToGraphQLOutputTypeMap(annotationToGraphQLOutputTypeMap)
                .withAnnotationToDataFetcherFactoryMap(annotationToDataFetcherFactoryMap)
                .withOverrides(overrides)
                // add the relay extra features
                .withRelay(relay)
                .addCustomFieldOutputTypeFunc(ForwardPagingArguments.class, pagingOutputTypeConverter)
                .addCustomFieldOutputTypeFunc(RelayNode.class, relayNodeOutputTypeFunc)
                .addCustomFieldArgumentsFunc(ForwardPagingArguments.class, customFieldArgumentsFunc)
                .build();

        // init TypeRegistry on the converters
        pagingOutputTypeConverter.setTypeRegistry(typeRegistry);

        // instantiate RelayHelper
        RelayHelper relayHelper = new RelayHelper(relay, typeRegistry, nodeFetcherService);

        // init RelayHelper on the converters
        pagingOutputTypeConverter.setRelayHelper(relayHelper);
        relayNodeOutputTypeFunc.setRelayHelper(relayHelper);

        return new Glitr(typeRegistry, relayHelper);
    }
}
