package com.nfl.glitr.data.mutation;

import com.nfl.glitr.annotation.GlitrDescription;
import com.nfl.glitr.annotation.GlitrNonNull;
import com.nfl.glitr.registry.mutation.RelayMutationType;

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
