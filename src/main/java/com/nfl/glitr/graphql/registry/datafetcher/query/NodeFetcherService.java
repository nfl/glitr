package com.nfl.glitr.graphql.registry.datafetcher.query;

import graphql.schema.DataFetcher;

public interface NodeFetcherService extends DataFetcher {

    Object getNodeById(String id);
}
