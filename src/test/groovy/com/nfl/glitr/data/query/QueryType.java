package com.nfl.glitr.data.query;

import com.nfl.glitr.annotation.GlitrArgument;
import com.nfl.glitr.annotation.GlitrForwardPagingArguments;

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

    @GlitrArgument(name = "id", type = String.class, nullable = false)
    public com.nfl.glitr.relay.Node getNode() { return null; }

    @GlitrArgument(name = "ids", type = String[].class, nullable = false)
    public List<com.nfl.glitr.relay.Node> getNodes() { return null; }
}
