package com.nfl.dm.shield.graphql.data.mutation;

import com.nfl.dm.shield.graphql.registry.mutation.RelayMutationType;

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
