package com.nfl.glitr;

import com.nfl.glitr.annotation.GlitrForwardPagingArguments;
import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.registry.TypeRegistryBuilder;
import com.nfl.glitr.registry.datafetcher.AnnotationBasedDataFetcherFactory;
import com.nfl.glitr.relay.RelayConfig;
import com.nfl.glitr.relay.RelayHelper;
import com.nfl.glitr.relay.type.CustomFieldArgumentsFunc;
import com.nfl.glitr.relay.type.PagingOutputTypeConverter;
import com.nfl.glitr.util.ObjectMapper;
import graphql.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlitrBuilder {

    private static final Logger logger = LoggerFactory.getLogger(GlitrBuilder.class);

    private Map<Class, List<Object>> overrides = new HashMap<>();
    private Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap = new HashMap<>();
    private Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap = new HashMap<>();
    private Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap = new HashMap<>();
    private RelayConfig relayConfig = null;
    private Object queryRoot = null;
    private Object mutationRoot = null;
    private ObjectMapper objectMapper = null;

    private GlitrBuilder() {
    }

    public GlitrBuilder withObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

    public GlitrBuilder withRelay(RelayConfig relayConfig) {
        this.relayConfig = relayConfig;
        return this;
    }

    public GlitrBuilder withRelay() {
        return withRelay(RelayConfig.newRelayConfig().build());
    }

    public static GlitrBuilder newGlitr() {
        return new GlitrBuilder();
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

        if (relayConfig != null) {
            return buildGlitrWithRelaySupport();
        }
        return buildGlitr();
    }

    private Glitr buildGlitr() {

        if (objectMapper == null) {
            logger.warn("No ObjectMapper instance has been registered.");
        }

        // create TypeRegistry
        TypeRegistry typeRegistry = TypeRegistryBuilder.newTypeRegistry()
                .withAnnotationToArgumentsProviderMap(annotationToArgumentsProviderMap)
                .withAnnotationToGraphQLOutputTypeMap(annotationToGraphQLOutputTypeMap)
                .withAnnotationToDataFetcherFactoryMap(annotationToDataFetcherFactoryMap)
                .withAnnotationToDataFetcherMap(annotationToDataFetcherMap)
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

        return new Glitr(typeRegistry, schema, objectMapper);
    }

    private Glitr buildGlitrWithRelaySupport() {

        if (objectMapper == null) {
            throw new IllegalArgumentException("No ObjectMapper instance has been registered. It's required to register one when using GLiTR with Relay");
        }

        PagingOutputTypeConverter pagingOutputTypeConverter = new PagingOutputTypeConverter();
        CustomFieldArgumentsFunc customFieldArgumentsFunc = new CustomFieldArgumentsFunc();

        // instantiate TypeRegistry
        TypeRegistry typeRegistry = TypeRegistryBuilder.newTypeRegistry()
                .withAnnotationToArgumentsProviderMap(annotationToArgumentsProviderMap)
                .withAnnotationToGraphQLOutputTypeMap(annotationToGraphQLOutputTypeMap)
                .withAnnotationToDataFetcherFactoryMap(annotationToDataFetcherFactoryMap)
                .withAnnotationToDataFetcherMap(annotationToDataFetcherMap)
                .withOverrides(overrides)
                // add the relay extra features
                .withExplicitRelayNodeScan(relayConfig.isExplicitRelayNodeScanEnabled())
                .withRelay(relayConfig.getRelay())
                .addCustomFieldOutputTypeFunc(GlitrForwardPagingArguments.class, pagingOutputTypeConverter)
                .addCustomFieldArgumentsFunc(GlitrForwardPagingArguments.class, customFieldArgumentsFunc)
                .build();

        // init TypeRegistry on the converters
        pagingOutputTypeConverter.setTypeRegistry(typeRegistry);

        // instantiate RelayHelper
        RelayHelper relayHelper = new RelayHelper(relayConfig.getRelay(), typeRegistry);

        // init RelayHelper on the converters
        pagingOutputTypeConverter.setRelayHelper(relayHelper);

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

        return new Glitr(typeRegistry, schema, objectMapper, relayHelper);
    }
}
