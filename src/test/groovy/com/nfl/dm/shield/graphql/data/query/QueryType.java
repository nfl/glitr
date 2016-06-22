package com.nfl.dm.shield.graphql.data.query;

import com.nfl.dm.shield.graphql.domain.graph.annotation.Argument;
import com.nfl.dm.shield.graphql.domain.graph.annotation.ForwardPagingArguments;
import com.nfl.dm.shield.graphql.registry.RelayNode;

import java.util.List;

public class QueryType {

    @ForwardPagingArguments
    public List<Video> getVideos() {
        return null;
    }

    @Argument(name = "id", type = String.class , nullable = false)
    public Video getVideo() {
        return null;
    }

    @RelayNode
    @Argument(name = "id", type = String.class, nullable = false)
    public Object getNode() { return null; }
}
