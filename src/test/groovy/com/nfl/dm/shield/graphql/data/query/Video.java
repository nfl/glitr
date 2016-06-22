package com.nfl.dm.shield.graphql.data.query;

import com.nfl.dm.shield.graphql.data.mutation.Bitrate;

import java.util.List;

public class Video {

    private String id;
    private String title;
    private String url;
    private List<Bitrate> bitrateList;

    public List<Bitrate> getBitrateList() {
        return bitrateList;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
