package com.nfl.dm.shield.graphql.data.mutation;

public class VideoMutationOut {

    private String title;
    private String id;
    private String url;
    private String bitrateList;

    public String getBitrateList() {
        return bitrateList;
    }

    public VideoMutationOut setBitrateList(String bitrateList) {
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
