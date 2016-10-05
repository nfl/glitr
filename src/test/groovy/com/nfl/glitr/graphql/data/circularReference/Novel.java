package com.nfl.glitr.graphql.data.circularReference;

public class Novel extends AbstractRead {

    private String title;
    private boolean reviewed;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }
}
