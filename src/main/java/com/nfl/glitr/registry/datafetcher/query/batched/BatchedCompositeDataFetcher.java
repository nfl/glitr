package com.nfl.glitr.registry.datafetcher.query.batched;

import com.nfl.glitr.registry.datafetcher.query.AbstractCompositeDataFetcher;
import graphql.execution.batched.BatchedDataFetcher;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The only difference with {@code CompositeDataFetcher} is it implements the BatchedDataFetcher interface to be
 * picked up by the {@code BatchedExecutionStrategy}
 */
public class BatchedCompositeDataFetcher extends AbstractCompositeDataFetcher implements BatchedDataFetcher {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(BatchedCompositeDataFetcher.class);


    public BatchedCompositeDataFetcher(DataFetcher[] fetchers) {
        super(fetchers);
    }

    public BatchedCompositeDataFetcher(List<DataFetcher> fetchers) {
        super(fetchers);
    }
}
