package com.nfl.glitr.data.query;

import com.nfl.glitr.annotation.GlitrArgument;
import com.nfl.glitr.annotation.GlitrForwardPagingArguments;
import com.nfl.glitr.annotation.GlitrQueryComplexity;

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
    public com.nfl.glitr.relay.Node getNode() {
        return null;
    }

    @GlitrArgument(name = "ids", type = String[].class, nullable = false)
    public List<com.nfl.glitr.relay.Node> getNodes() {
        return null;
    }

    @GlitrForwardPagingArguments
    public List<Video> getOtherVideos() {
        return null;
    }

    @GlitrArgument(name = "ids", type = String[].class, nullable = false)
    public List<com.nfl.glitr.relay.Node> getZZZNodes() {
        return null;
    }

    // no arguments
    public List<Video> getZZZVideos() {
        return null;
    }


    //Query complexity formula declarations

    @GlitrQueryComplexity("#{depth}")
    public List<Video> getVideosDepth() {
        return null;
    }

    @GlitrQueryComplexity("#{childScore}")
    public List<Video> getChildScore() {
        return null;
    }

    @GlitrQueryComplexity("#{currentCollectionSize}")
    public List<Video> getCurrentCollectionSize() {
        return null;
    }

    @GlitrQueryComplexity("incorrectVariableDeclaration + 5")
    public List<Video> getIncorrectVariableDeclaration() {
        return null;
    }

    @GlitrQueryComplexity("#{childScore} + #{childScore} * #{childScore}")
    public List<Video> getDuplicateVariables() {
        return null;
    }

    @GlitrQueryComplexity(ignore = true)
    public List<Video> getIgnore() {
        return null;
    }
}
