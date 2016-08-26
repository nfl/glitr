package com.nfl.dm.shield.graphql.data.query;

import com.nfl.dm.shield.graphql.domain.graph.annotation.GlitrArgument;
import com.nfl.dm.shield.graphql.domain.graph.annotation.GlitrForwardPagingArguments;
import com.nfl.dm.shield.graphql.registry.RelayNode;

import java.util.List;

public class QueryType {

    @GlitrForwardPagingArguments
    public List<Video> getVideos() {
        return null;
    }

    @GlitrArgument(name = "id", type = String.class , nullable = false)
    public Video getVideo() {
        return null;
    }

    @RelayNode
    @GlitrArgument(name = "id", type = String.class, nullable = false)
    public Object getNode() { return null; }
}
