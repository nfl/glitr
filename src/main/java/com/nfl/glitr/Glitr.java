package com.nfl.glitr;

import com.nfl.glitr.calculator.QueryComplexityCalculator;
import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.relay.RelayHelper;
import com.nfl.glitr.util.ObjectMapper;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.visibility.GraphqlFieldVisibility;

import javax.annotation.Nullable;

import static graphql.Assert.assertNotNull;
import static java.util.Objects.nonNull;

public class Glitr {

    private final TypeRegistry typeRegistry;
    private final GraphQLCodeRegistry.Builder codeRegistryBuilder;
    private final RelayHelper relayHelper;
    private GraphQLSchema schema;
    private QueryComplexityCalculator queryComplexityCalculator;
    private static ObjectMapper objectMapper;


    public Glitr(TypeRegistry typeRegistry,
                 GraphQLCodeRegistry.Builder codeRegistryBuilder,
                 Class queryRoot,
                 @Nullable GraphqlFieldVisibility fieldVisibility,
                 @Nullable ObjectMapper objectMapper,
                 @Nullable Class mutationRoot,
                 @Nullable Class subscriptionRoot,
                 @Nullable QueryComplexityCalculator queryComplexityCalculator) {
        this(typeRegistry, codeRegistryBuilder, queryRoot, fieldVisibility, objectMapper, null, mutationRoot, subscriptionRoot, queryComplexityCalculator);
    }

    public Glitr(TypeRegistry typeRegistry,
                 GraphQLCodeRegistry.Builder codeRegistryBuilder,
                 Class queryRoot,
                 @Nullable GraphqlFieldVisibility fieldVisibility,
                 @Nullable ObjectMapper objectMapper,
                 @Nullable RelayHelper relayHelper,
                 @Nullable Class mutationRoot,
                 @Nullable Class subscriptionRoot,
                 @Nullable QueryComplexityCalculator queryComplexityCalculator) {
        this.codeRegistryBuilder = assertNotNull(codeRegistryBuilder, "codeRegistryBuilder can't be null");
        this.typeRegistry = assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(queryRoot, "queryRoot class can't be null");
        this.relayHelper = relayHelper;

        Glitr.objectMapper = objectMapper;
        this.schema = buildSchema(queryRoot, mutationRoot, subscriptionRoot, fieldVisibility);

        if (nonNull(queryComplexityCalculator)) {
            this.queryComplexityCalculator = queryComplexityCalculator.withSchema(this.schema);
        }
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    @Nullable
    public QueryComplexityCalculator getQueryComplexityCalculator() {
        return queryComplexityCalculator;
    }

    @Nullable
    public RelayHelper getRelayHelper() {
        return relayHelper;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            throw new RuntimeException("Serialization Impossible. Can't find an ObjectMapper implementation. Please configure GLiTR to use an ObjectMapper.");
        }
        return objectMapper;
    }

    private GraphQLSchema buildSchema(Class queryRoot, Class mutationRoot, Class subscriptionRoot, GraphqlFieldVisibility fieldVisibility) {
        // create GraphQL Schema
        GraphQLObjectType mutationType = null;
        if (mutationRoot != null) {
            mutationType = typeRegistry.createRelayMutationType(mutationRoot);
        }
        GraphQLObjectType subscriptionType = null;
        if (subscriptionRoot != null) {
            subscriptionType = (GraphQLObjectType) typeRegistry.lookup(subscriptionRoot);
        }
        GraphQLObjectType queryType = (GraphQLObjectType) typeRegistry.lookup(queryRoot);

        if (fieldVisibility != null) {
            codeRegistryBuilder.fieldVisibility(fieldVisibility);

            return GraphQLSchema.newSchema()
                    .query(queryType)
                    .mutation(mutationType)
                    .subscription(subscriptionType)
                    .additionalTypes(typeRegistry.getTypeDictionary())
                    .codeRegistry(codeRegistryBuilder.build())
                    .build();
        }

        return GraphQLSchema.newSchema()
                .query(queryType)
                .mutation(mutationType)
                .subscription(subscriptionType)
                .additionalTypes(typeRegistry.getTypeDictionary())
                .codeRegistry(codeRegistryBuilder.build())
                .build();
    }

    public GraphQLSchema reloadSchema(Class queryRoot, Class mutationRoot, Class subscriptionRoot, GraphqlFieldVisibility fieldVisibility) {
        this.schema = buildSchema(queryRoot, mutationRoot, subscriptionRoot, fieldVisibility);
        return this.schema;
    }
}
