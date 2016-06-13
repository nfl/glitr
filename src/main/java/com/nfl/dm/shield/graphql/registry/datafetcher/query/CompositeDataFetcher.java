package com.nfl.dm.shield.graphql.registry.datafetcher.query;

import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CompositeDataFetcher extends AbstractCompositeDataFetcher {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(CompositeDataFetcher.class);

    public CompositeDataFetcher(DataFetcher[] fetchers) {
        super(fetchers);
    }

    public CompositeDataFetcher(List<DataFetcher> fetchers) {
        super(fetchers);
    }
}
