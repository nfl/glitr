package com.nfl.glitr.graphql.data.mutation;

import com.nfl.glitr.graphql.registry.mutation.RelayMutationType;

public class VideoMutationPayload extends RelayMutationType {

    private VideoMutationOut videoMutationPayload;

    public VideoMutationOut getVideoMutationPayload() {
        return videoMutationPayload;
    }

    public VideoMutationPayload setVideoMutationPayload(VideoMutationOut videoMutationPayload) {
        this.videoMutationPayload = videoMutationPayload;
        return this;
    }
}
