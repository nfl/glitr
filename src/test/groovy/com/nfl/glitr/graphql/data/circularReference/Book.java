package com.nfl.glitr.graphql.data.circularReference;

public class Book implements Readable {
    private String title;
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
