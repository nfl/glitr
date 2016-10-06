package com.nfl.glitr.data.mutation;

import java.util.List;

public class VideoMutationIn {

    private String title;
    private String url;
    private List<Bitrate> bitrateList;

    public List<Bitrate> getBitrateList() {
        return bitrateList;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public VideoMutationIn setBitrateList(List<Bitrate> bitrateList) {
        this.bitrateList = bitrateList;
        return this;
    }

    public VideoMutationIn setTitle(String title) {
        this.title = title;
        return this;
    }

    public VideoMutationIn setUrl(String url) {
        this.url = url;
        return this;
    }
}
