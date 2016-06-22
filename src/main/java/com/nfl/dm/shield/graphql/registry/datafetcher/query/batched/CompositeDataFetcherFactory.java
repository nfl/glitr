package com.nfl.dm.shield.graphql.registry.datafetcher.query.batched;

import com.nfl.dm.shield.graphql.ReflectionUtil;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.CompositeDataFetcher;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.OverrideDataFetcher;
import graphql.execution.batched.Batched;
import graphql.execution.batched.UnbatchedDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.FieldDataFetcher;
import graphql.schema.PropertyDataFetcher;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Create a CompositeDataFetcher based on the supplied DataFetchers.
 * The logic is as follows:
 * If all the data fetchers are batched (exclude PropertyDataFetcher or OverrideDataFetcher) then it will create a BatchedCompositeDataFetcher
 * If all the data fetchers are non batched then it will create a CompositeDataFetcher
 * If both types of data fetchers are detected, IllegalArgumentException is thrown.
 */
public class CompositeDataFetcherFactory {

    public static DataFetcher create(final List<DataFetcher> supplied) {
        checkArgument(supplied != null && !supplied.isEmpty());

        // convert OverrideDataFetcher to batched one if the overrideMethod is @Batched
        List<DataFetcher> fetchers = supplied.stream()
                // filter out all the OverrideDataFetchers that have a null overrideMethod - reason is type registry adds
                // default overrideDF
                .filter(f -> !(f instanceof OverrideDataFetcher) || ((OverrideDataFetcher)f).getOverrideMethod() != null)
                .map(f -> {
                    if (f instanceof OverrideDataFetcher) {
                        OverrideDataFetcher overrideDataFetcher = ((OverrideDataFetcher) f);
                        Method overrideMethod = overrideDataFetcher.getOverrideMethod();
                        Batched batched = overrideMethod.getAnnotation(Batched.class);
                        if (batched != null) {
                            return new UnbatchedDataFetcher(overrideDataFetcher);
                        }
                    }
                    return f;
        }).collect(Collectors.toList());


        // let's see if there is at least one Batched in the list
        boolean isListContainsBatchedDataFetcher = fetchers.stream()
                .filter(f -> !(f instanceof PropertyDataFetcher) && !(f instanceof FieldDataFetcher))
                .anyMatch(ReflectionUtil::isDataFetcherBatched);

        if (!isListContainsBatchedDataFetcher) {
            return new CompositeDataFetcher(fetchers);
        }

        // are they all batched?
        boolean isListOnlyBatchedDataFetcher = fetchers.stream()
                .filter(f -> !(f instanceof PropertyDataFetcher) && !(f instanceof FieldDataFetcher))
                .allMatch(ReflectionUtil::isDataFetcherBatched);


        if (!isListOnlyBatchedDataFetcher) {
            throw new IllegalArgumentException("Both Batched and Simple data fetchers detected in passed list. " +
                                                       "Batched data fetchers can't mix with simple ones" + fetchers);
        }

        // convert PropertyDataFetcher to batched one
        List<DataFetcher> batchedDataFetchers = fetchers.stream().map(f -> {
            if (f instanceof PropertyDataFetcher) {
                return new UnbatchedDataFetcher(f);
            }
            return f;
        }).collect(Collectors.toList());
        return new BatchedCompositeDataFetcher(batchedDataFetchers);

    }
}
