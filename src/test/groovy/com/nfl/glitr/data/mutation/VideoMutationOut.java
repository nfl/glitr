package com.nfl.glitr.data.mutation;

import com.nfl.glitr.annotation.GlitrQueryComplexity;

import java.util.List;

public class VideoMutationOut {

    private String title;
    private String id;
    private String url;
    @GlitrQueryComplexity("4")
    private List<Bitrate> bitrateList;


    public List<Bitrate> getBitrateList() {
        return bitrateList;
    }

    public VideoMutationOut setBitrateList(List<Bitrate> bitrateList) {
        this.bitrateList = bitrateList;
        return this;
    }

    public String getId() {
        return id;
    }

    public VideoMutationOut setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public VideoMutationOut setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public VideoMutationOut setUrl(String url) {
        this.url = url;
        return this;
    }
}
