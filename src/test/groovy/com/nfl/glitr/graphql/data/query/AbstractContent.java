package com.nfl.glitr.graphql.data.query;

public abstract class AbstractContent extends AbstractTimestamped {
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
