package com.nfl.glitr.registry.datafetcher.query;

import com.nfl.glitr.registry.datafetcher.query.CompositeDataFetcher;
import com.nfl.glitr.util.ReflectionUtil;
import com.nfl.glitr.registry.datafetcher.query.OverrideDataFetcher;
import graphql.execution.batched.Batched;
import graphql.execution.batched.UnbatchedDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.PropertyDataFetcher;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class CompositeDataFetcherFactory {

    public static DataFetcher create(final List<DataFetcher> supplied) {
        checkArgument(supplied != null && !supplied.isEmpty());

        /**
         * Since {@link graphql.execution.batched.BatchedDataFetcher} and {@link graphql.execution.batched.UnbatchedDataFetcher}
         * are being deprecated, we defaulted it to {@link CompositeDataFetcher}
         */
        return new CompositeDataFetcher(supplied);
    }
}
