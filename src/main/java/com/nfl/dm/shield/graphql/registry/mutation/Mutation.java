package com.nfl.dm.shield.graphql.registry.mutation;

import graphql.schema.DataFetchingEnvironment;

public interface Mutation<I, R> {

    R call(I input, DataFetchingEnvironment env);
}
