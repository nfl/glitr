package com.nfl.dm.shield.graphql;

import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import com.nfl.dm.shield.graphql.relay.RelayHelper;
import graphql.schema.GraphQLSchema;

import javax.annotation.Nullable;

import static graphql.Assert.assertNotNull;


public class Glitr {

    private TypeRegistry typeRegistry;
    private RelayHelper relayHelper;
    private GraphQLSchema schema;


    Glitr(TypeRegistry typeRegistry, @Nullable RelayHelper relayHelper, GraphQLSchema schema) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(schema, "GraphQLSchema can't be null");
        this.typeRegistry = typeRegistry;
        this.relayHelper = relayHelper;
        this.schema = schema;
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
}
