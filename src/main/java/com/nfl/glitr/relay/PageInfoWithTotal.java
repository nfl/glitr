package com.nfl.glitr.relay;

import graphql.relay.PageInfo;

public class PageInfoWithTotal extends PageInfo {

    private int total;


    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
