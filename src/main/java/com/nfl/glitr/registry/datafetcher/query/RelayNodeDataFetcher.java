package com.nfl.glitr.registry.datafetcher.query;

import graphql.schema.DataFetcher;

public interface RelayNodeDataFetcher extends DataFetcher {

    Object getNodeById(String id);
}
