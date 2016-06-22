package com.nfl.dm.shield.graphql.registry.mutation;

import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull;

public abstract class RelayMutationType {
    @GraphQLNonNull
    private String clientMutationId;

    public String getClientMutationId() {
        return clientMutationId;
    }

    public RelayMutationType setClientMutationId(String clientMutationId) {
        this.clientMutationId = clientMutationId;
        return this;
    }
}