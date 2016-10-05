package com.nfl.glitr.graphql.data.query;

import com.nfl.glitr.graphql.data.mutation.Bitrate;

import java.util.List;

public class Video extends AbstractContent implements Playable {

    private String id;
    private String url;
    private List<Bitrate> bitrateList;

    public List<Bitrate> getBitrateList() {
        return bitrateList;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

}
