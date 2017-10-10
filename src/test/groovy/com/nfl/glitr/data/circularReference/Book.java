package com.nfl.glitr.data.circularReference;

import com.nfl.glitr.annotation.GlitrQueryComplexity;

public class Book implements Readable {

    private String title;
    @GlitrQueryComplexity("12")
    private Book synopsis;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Book getSynopsis() {
        return synopsis;
    }


    public void setSynopsis(Book synopsis) {
        this.synopsis = synopsis;
    }


}
