package com.nfl.dm.shield.graphql.data.query;

import com.nfl.dm.shield.graphql.domain.graph.annotation.GlitrDescription;

@GlitrDescription("Node interface needed for Relay")
public interface Node {
    String getId();
}
