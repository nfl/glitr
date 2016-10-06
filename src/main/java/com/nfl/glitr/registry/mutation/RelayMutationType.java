package com.nfl.glitr.registry.mutation;

import com.nfl.glitr.annotation.GlitrNonNull;

/**
 * Relay requires the notion of an Id passed by the client and is returned back to allow identification and
 * association of mutations
 */
public abstract class RelayMutationType {

    @GlitrNonNull
    private String clientMutationId;


    public String getClientMutationId() {
        return clientMutationId;
    }

    public RelayMutationType setClientMutationId(String clientMutationId) {
        this.clientMutationId = clientMutationId;
        return this;
    }
}