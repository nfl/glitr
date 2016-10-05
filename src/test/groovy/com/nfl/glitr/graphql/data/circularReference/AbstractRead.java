package com.nfl.glitr.graphql.data.circularReference;

public abstract class AbstractRead {
    private int pageCount;
    private Novel novel;

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public Novel getNovel() {
        return novel;
    }

    public void setNovel(Novel novel) {
        this.novel = novel;
    }
}
