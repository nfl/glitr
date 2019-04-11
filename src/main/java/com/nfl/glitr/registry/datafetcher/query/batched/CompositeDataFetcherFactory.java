package com.nfl.glitr.registry.datafetcher.query.batched;

import com.nfl.glitr.registry.datafetcher.query.CompositeDataFetcher;
import com.nfl.glitr.registry.datafetcher.query.OverrideDataFetcher;
import graphql.schema.DataFetcher;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Create a CompositeDataFetcher based on the supplied DataFetchers.
 *
 */
public class CompositeDataFetcherFactory {

    public static DataFetcher create(final List<DataFetcher> supplied) {

        List<DataFetcher> fetchers = supplied.stream()
                // filter out all the OverrideDataFetchers that have a null overrideMethod since type registry adds a default overrideDF
                .filter(f -> !(f instanceof OverrideDataFetcher) || ((OverrideDataFetcher)f).getOverrideMethod() != null)
                .collect(Collectors.toList());

        return new CompositeDataFetcher(fetchers);
    }
}
