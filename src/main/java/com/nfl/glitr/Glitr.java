package com.nfl.glitr;

import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.relay.RelayHelper;
import com.nfl.glitr.util.ObjectMapper;
import com.nfl.glitr.calculator.QueryComplexityCalculator;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.visibility.GraphqlFieldVisibility;

import javax.annotation.Nullable;

import static graphql.Assert.assertNotNull;
import static java.util.Objects.nonNull;

public class Glitr {

    private TypeRegistry typeRegistry;
    private RelayHelper relayHelper;
    private GraphQLSchema schema;
    private QueryComplexityCalculator queryComplexityCalculator;
    private static ObjectMapper objectMapper;


    public Glitr(TypeRegistry typeRegistry, Class queryRoot, @Nullable GraphqlFieldVisibility fieldVisibility, @Nullable ObjectMapper objectMapper, @Nullable Class mutationRoot, @Nullable QueryComplexityCalculator queryComplexityCalculator) {
        this(typeRegistry, queryRoot, fieldVisibility, objectMapper, null, mutationRoot, queryComplexityCalculator);
    }

    public Glitr(TypeRegistry typeRegistry, Class queryRoot, @Nullable GraphqlFieldVisibility fieldVisibility, @Nullable ObjectMapper objectMapper, @Nullable RelayHelper relayHelper, @Nullable Class mutationRoot, @Nullable QueryComplexityCalculator queryComplexityCalculator) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(queryRoot, "queryRoot class can't be null");
        this.typeRegistry = typeRegistry;
        this.relayHelper = relayHelper;
        if (nonNull(queryComplexityCalculator)) {
            this.queryComplexityCalculator = queryComplexityCalculator.
                    withQueryComplexityMultipliersMap(typeRegistry.getQueryComplexityMultipliersMap())
                    .withQueryComplexityExcludeNodes(typeRegistry.getQueryComplexityExcludeNodes());
        }
        Glitr.objectMapper = objectMapper;
        this.schema = buildSchema(queryRoot, mutationRoot, fieldVisibility);
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

    private GraphQLSchema buildSchema(Class queryRoot, Class mutationRoot, GraphqlFieldVisibility fieldVisibility) {
        // create GraphQL Schema
        GraphQLObjectType mutationType = null;
        if (mutationRoot != null) {
            mutationType = typeRegistry.createRelayMutationType(mutationRoot);
        }

        GraphQLObjectType queryType = (GraphQLObjectType) typeRegistry.lookup(queryRoot);

        if (fieldVisibility != null) {
            return GraphQLSchema.newSchema()
                    .query(queryType)
                    .mutation(mutationType)
                    .fieldVisibility(fieldVisibility)
                    .build(typeRegistry.getTypeDictionary());
        }

        return GraphQLSchema.newSchema()
                .query(queryType)
                .mutation(mutationType)
                .build(typeRegistry.getTypeDictionary());
    }

    public GraphQLSchema reloadSchema(Class queryRoot, Class mutationRoot, GraphqlFieldVisibility fieldVisibility) {
        this.schema = buildSchema(queryRoot, mutationRoot, fieldVisibility);
        return this.schema;
    }
}
