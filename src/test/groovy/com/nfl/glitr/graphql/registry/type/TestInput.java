package com.nfl.glitr.graphql.registry.type;

import com.nfl.glitr.graphql.data.mutation.Bitrate;

public class TestInput {

    private String id;
    private String url;
    private Bitrate bitrate;

    public String getId() {
        return id;
    }
    public String getUrl() {
        return url;
    }
    public Bitrate getBitrate() {
        return bitrate;
    }
}
