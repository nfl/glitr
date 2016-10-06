package com.nfl.glitr.registry.mutation;

import graphql.schema.DataFetchingEnvironment;

public interface Mutation<I, R> {

    R call(I input, DataFetchingEnvironment env);
}
