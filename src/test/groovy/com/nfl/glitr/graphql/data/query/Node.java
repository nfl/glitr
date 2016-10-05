package com.nfl.glitr.graphql.data.query;

import com.nfl.glitr.graphql.domain.graph.annotation.GlitrDescription;

@GlitrDescription("Node interface needed for Relay")
public interface Node {
    String getId();
}
