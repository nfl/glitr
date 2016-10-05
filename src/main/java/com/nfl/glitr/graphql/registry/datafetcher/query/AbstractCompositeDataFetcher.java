package com.nfl.glitr.graphql.registry.datafetcher.query;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;

/**
 * The {@code CompositeDataFetcher} takes in a list of data fetchers at startup time that are to be tried sequentially.
 * Taking a list instead of a single data fetcher allows:
 *      1. One or more data fetchers to serve as backup. (e.g: if this element isn't there try this other data fetcher that fetches from another source)
 *      2. It also allows the system to be more lenient if multiple ways of fetching have been defined but only one was intended
 *          (e.g: {@code @Relationship} is present on a getter method but the developer might want to override this fetcher without having to remove the annotation)
 */
public abstract class AbstractCompositeDataFetcher implements DataFetcher {

    protected DataFetcher[] fetchers;


    public AbstractCompositeDataFetcher(DataFetcher[] fetchers) {
        this.fetchers = fetchers;
    }

    public AbstractCompositeDataFetcher(List<DataFetcher> fetchers) {
        this(fetchers.toArray(new DataFetcher[fetchers.size()]));
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        for (DataFetcher fetcher : fetchers) {
            Object result = fetcher.get(environment);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public DataFetcher[] getFetchers() {
        return fetchers;
    }
}
