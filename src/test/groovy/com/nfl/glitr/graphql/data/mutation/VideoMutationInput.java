package com.nfl.glitr.graphql.data.mutation;

import com.nfl.glitr.graphql.domain.graph.annotation.GlitrDescription;
import com.nfl.glitr.graphql.domain.graph.annotation.GlitrNonNull;
import com.nfl.glitr.graphql.registry.mutation.RelayMutationType;

@GlitrDescription("Relay mutation input")
public class VideoMutationInput extends RelayMutationType {

    private VideoMutationIn videoMutation;

    @GlitrDescription("Info meta data needed")
    @GlitrNonNull
    public VideoMutationIn getVideoMutation() {
        return videoMutation;
    }

    public VideoMutationInput setVideoMutation(VideoMutationIn videoMutation) {
        this.videoMutation = videoMutation;
        return this;
    }
}
