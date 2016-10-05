package com.nfl.glitr.graphql;

import com.nfl.glitr.graphql.domain.graph.annotation.GlitrForwardPagingArguments;
import com.nfl.glitr.graphql.registry.RelayNode;
import com.nfl.glitr.graphql.registry.TypeRegistry;
import com.nfl.glitr.graphql.registry.TypeRegistryBuilder;
import com.nfl.glitr.graphql.registry.datafetcher.AnnotationBasedDataFetcherFactory;
import com.nfl.glitr.graphql.registry.datafetcher.query.NodeFetcherService;
import com.nfl.glitr.graphql.relay.Relay;
import com.nfl.glitr.graphql.relay.RelayHelper;
import com.nfl.glitr.graphql.relay.RelayImpl;
import com.nfl.glitr.graphql.relay.type.CustomFieldArgumentsFunc;
import com.nfl.glitr.graphql.relay.type.PagingOutputTypeConverter;
import com.nfl.glitr.graphql.relay.type.RelayNodeOutputTypeFunc;
import graphql.schema.*;
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
    private Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap = new HashMap<>();
    private Relay relay = null;

    private Object queryRoot = null;
    private Object mutationRoot = null;


    private GlitrBuilder() {
    }

    public GlitrBuilder withRelay(Relay relay) {
        this.relay = relay;
        return this;
    }

    public GlitrBuilder withQueryRoot(Object queryRoot) {
        this.queryRoot = queryRoot;
        return this;
    }

    public GlitrBuilder withMutationRoot(Object mutationRoot) {
        this.mutationRoot = mutationRoot;
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

    public GlitrBuilder addCustomDataFetcher(Class<? extends Annotation> annotationClass, DataFetcher dataFetcher) {
        annotationToDataFetcherMap.putIfAbsent(annotationClass, dataFetcher);
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
        return new GlitrBuilder().withRelay(new RelayImpl());
    }

    public Glitr build() {
        if (queryRoot == null) {
            throw new RuntimeException("A query entry point must be defined.");
        }

        // add overrides for root endpoints
        this.addOverride(queryRoot.getClass(), queryRoot);
        if (mutationRoot != null) {
            this.addOverride(mutationRoot.getClass(), mutationRoot);
        }

        if (relay != null) {
            return buildGlitrWithRelaySupport();
        }
        return buildGlitr();
    }

    private Glitr buildGlitr() {
        // create TypeRegistry
        TypeRegistry typeRegistry = TypeRegistryBuilder.newTypeRegistry()
                .withAnnotationToArgumentsProviderMap(annotationToArgumentsProviderMap)
                .withAnnotationToGraphQLOutputTypeMap(annotationToGraphQLOutputTypeMap)
                .withAnnotationToDataFetcherFactoryMap(annotationToDataFetcherFactoryMap)
                .withOverrides(overrides)
                .build();

        // create GraphQL Schema
        GraphQLObjectType mutationType = null;
        if (mutationRoot != null) {
            mutationType = typeRegistry.createRelayMutationType(mutationRoot.getClass());
        }

        GraphQLObjectType queryType = (GraphQLObjectType) typeRegistry.lookup(queryRoot.getClass());
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .mutation(mutationType)
                .build(typeRegistry.getTypeDictionary());

        return new Glitr(typeRegistry, null, schema);
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
                .withAnnotationToDataFetcherMap(annotationToDataFetcherMap)
                .withOverrides(overrides)
                // add the relay extra features
                .withRelay(relay)
                .addCustomFieldOutputTypeFunc(GlitrForwardPagingArguments.class, pagingOutputTypeConverter)
                .addCustomFieldOutputTypeFunc(RelayNode.class, relayNodeOutputTypeFunc)
                .addCustomFieldArgumentsFunc(GlitrForwardPagingArguments.class, customFieldArgumentsFunc)
                .build();

        // init TypeRegistry on the converters
        pagingOutputTypeConverter.setTypeRegistry(typeRegistry);

        // instantiate RelayHelper
        RelayHelper relayHelper = new RelayHelper(relay, typeRegistry, nodeFetcherService);

        // init RelayHelper on the converters
        pagingOutputTypeConverter.setRelayHelper(relayHelper);
        relayNodeOutputTypeFunc.setRelayHelper(relayHelper);

        // create GraphQL Schema
        GraphQLObjectType mutationType = null;
        if (mutationRoot != null) {
            mutationType = typeRegistry.createRelayMutationType(mutationRoot.getClass());
        }

        GraphQLObjectType queryType = (GraphQLObjectType) typeRegistry.lookup(queryRoot.getClass());
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .mutation(mutationType)
                .build(typeRegistry.getTypeDictionary());

        return new Glitr(typeRegistry, relayHelper, schema);
    }
}
