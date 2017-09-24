package com.nfl.glitr.relay;

import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultPageInfo;

public class PageInfoWithTotal extends DefaultPageInfo {

    private int total;

    public PageInfoWithTotal(ConnectionCursor startCursor, ConnectionCursor endCursor,
                             boolean hasPreviousPage, boolean hasNextPage) {
        super(startCursor, endCursor, hasPreviousPage, hasNextPage);
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
