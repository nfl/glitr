package com.nfl.glitr;

import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.relay.RelayHelper;
import com.nfl.glitr.util.ObjectMapper;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import javax.annotation.Nullable;

import static graphql.Assert.assertNotNull;

public class Glitr {

    private TypeRegistry typeRegistry;
    private RelayHelper relayHelper;
    private GraphQLSchema schema;
    private static ObjectMapper objectMapper;


    public Glitr(TypeRegistry typeRegistry, Class queryRoot, @Nullable ObjectMapper objectMapper, @Nullable Class mutationRoot) {
        this(typeRegistry, queryRoot, objectMapper, null, mutationRoot);
    }

    public Glitr(TypeRegistry typeRegistry, Class queryRoot, @Nullable ObjectMapper objectMapper, @Nullable RelayHelper relayHelper, @Nullable Class mutationRoot) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(typeRegistry, "queryRoot class can't be null");
        this.typeRegistry = typeRegistry;
        this.relayHelper = relayHelper;
        Glitr.objectMapper = objectMapper;
        this.schema = buildSchema(queryRoot, mutationRoot);
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
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

    private GraphQLSchema buildSchema(Class queryRoot, Class mutationRoot) {
        // create GraphQL Schema
        GraphQLObjectType mutationType = null;
        if (mutationRoot != null) {
            mutationType = typeRegistry.createRelayMutationType(mutationRoot);
        }

        GraphQLObjectType queryType = (GraphQLObjectType) typeRegistry.lookup(queryRoot);
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .mutation(mutationType)
                .build(typeRegistry.getTypeDictionary());
        return schema;
    }

    public GraphQLSchema reloadSchema(Class queryRoot, Class mutationRoot) {
        this.schema = buildSchema(queryRoot, mutationRoot);
        return this.schema;
    }
}
