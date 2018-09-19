package com.nfl.glitr.data.mutation;

import com.nfl.glitr.annotation.GlitrArgument;
import com.nfl.glitr.annotation.GlitrArguments;
import com.nfl.glitr.annotation.GlitrDescription;
import com.nfl.glitr.registry.mutation.RelayMutationDataFetcher;
import com.nfl.glitr.registry.mutation.RelayMutation;
import graphql.schema.DataFetchingEnvironment;

import static com.nfl.glitr.annotation.GlitrArgument.nullability.NON_NULL;

public class MutationType {

    @GlitrDescription("Saves Info related to a video")
    @GlitrArguments({@GlitrArgument(name = "input", type = VideoMutationInput.class, nullability = NON_NULL, defaultValue = "{default input}")})
    public VideoMutationPayload getSaveVideoInfoMutation(DataFetchingEnvironment env) {
        SaveVideoInfo saveVideoInfo = new SaveVideoInfo();
        RelayMutationDataFetcher relayMutationDataFetcher = new RelayMutationDataFetcher(VideoMutationInput.class, saveVideoInfo);
        return (VideoMutationPayload) relayMutationDataFetcher.get(env);
    }

    class SaveVideoInfo implements RelayMutation<VideoMutationInput, VideoMutationPayload> {
        @Override
        public VideoMutationPayload call(VideoMutationInput mtnInput, DataFetchingEnvironment env) {
            VideoMutationPayload out = new VideoMutationPayload();
            out.setVideoMutationPayload(new VideoMutationOut()
                    .setTitle(mtnInput.getVideoMutation().getTitle())
                    .setBitrateList(mtnInput.getVideoMutation().getBitrateList()));
            return out;
        }
    }
}
