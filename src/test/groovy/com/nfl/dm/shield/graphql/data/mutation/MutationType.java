package com.nfl.dm.shield.graphql.data.mutation;

import com.nfl.dm.shield.graphql.domain.graph.annotation.Argument;
import com.nfl.dm.shield.graphql.domain.graph.annotation.Arguments;
import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLDescription;
import com.nfl.dm.shield.graphql.registry.mutation.MutationDataFetcher;
import com.nfl.dm.shield.graphql.registry.mutation.RelayMutation;
import graphql.schema.DataFetchingEnvironment;

public class MutationType {

    @GraphQLDescription("Saves Info related to a video")
    @Arguments({@Argument(name = "input", type = VideoMutationInput.class, nullable = false, defaultValue = "{default input}")})
    public VideoMutationPayload getSaveVideoInfoMutation(DataFetchingEnvironment env) {
        SaveVideoInfo saveVideoInfo = new SaveVideoInfo();
        MutationDataFetcher mutationDataFetcher = new MutationDataFetcher(VideoMutationInput.class, null, saveVideoInfo);
        return (VideoMutationPayload) mutationDataFetcher.get(env);
    }

    class SaveVideoInfo implements RelayMutation<VideoMutationInput, VideoMutationPayload> {
        @Override
        public VideoMutationPayload call(VideoMutationInput mtnInput, DataFetchingEnvironment env) {
            VideoMutationPayload out = new VideoMutationPayload();
            out.setVideoMutationPayload(new VideoMutationOut().setTitle(mtnInput.getVideoMutation().getTitle()));
            return out;
        }
    }
}
