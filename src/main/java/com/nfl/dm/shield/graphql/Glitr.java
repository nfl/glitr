package com.nfl.dm.shield.graphql;

import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import com.nfl.dm.shield.graphql.relay.RelayHelper;

import javax.annotation.Nullable;

import static graphql.Assert.assertNotNull;


public class Glitr {

    private TypeRegistry typeRegistry;
    private RelayHelper relayHelper;

    Glitr(TypeRegistry typeRegistry, @Nullable RelayHelper relayHelper) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        this.typeRegistry = typeRegistry;
        this.relayHelper = relayHelper;
    }

    public TypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    @Nullable
    public RelayHelper getRelayHelper() {
        return relayHelper;
    }
}
