package com.nfl.glitr.data.circularReference;

import com.nfl.glitr.annotation.GlitrQueryComplexity;

public abstract class AbstractRead {

    private int pageCount;
    @GlitrQueryComplexity("12")
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
