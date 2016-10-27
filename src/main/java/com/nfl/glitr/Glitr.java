package com.nfl.glitr;

import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.relay.RelayHelper;
import com.nfl.glitr.util.ObjectMapper;
import graphql.schema.GraphQLSchema;

import javax.annotation.Nullable;

import static graphql.Assert.assertNotNull;

public class Glitr {

    private TypeRegistry typeRegistry;
    private RelayHelper relayHelper;
    private GraphQLSchema schema;
    private static ObjectMapper objectMapper;


    Glitr(TypeRegistry typeRegistry, GraphQLSchema schema) {
        this(typeRegistry, schema, null);
    }

    Glitr(TypeRegistry typeRegistry, GraphQLSchema schema, @Nullable ObjectMapper objectMapper) {
        this(typeRegistry, schema, objectMapper, null);
    }

    Glitr(TypeRegistry typeRegistry, GraphQLSchema schema, @Nullable ObjectMapper objectMapper, @Nullable RelayHelper relayHelper) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(schema, "GraphQLSchema can't be null");
        this.typeRegistry = typeRegistry;
        this.relayHelper = relayHelper;
        this.schema = schema;
        Glitr.objectMapper = objectMapper;
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
}
