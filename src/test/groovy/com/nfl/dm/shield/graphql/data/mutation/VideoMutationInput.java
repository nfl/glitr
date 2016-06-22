package com.nfl.dm.shield.graphql.data.mutation;

import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLDescription;
import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull;
import com.nfl.dm.shield.graphql.registry.mutation.RelayMutationType;

@GraphQLDescription("Relay mutation input")
public class VideoMutationInput extends RelayMutationType {

    private VideoMutationIn videoMutation;

    @GraphQLDescription("Info meta data needed")
    @GraphQLNonNull
    public VideoMutationIn getVideoMutation() {
        return videoMutation;
    }

    public VideoMutationInput setVideoMutation(VideoMutationIn videoMutation) {
        this.videoMutation = videoMutation;
        return this;
    }
}
