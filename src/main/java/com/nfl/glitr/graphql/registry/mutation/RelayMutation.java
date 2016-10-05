package com.nfl.glitr.graphql.registry.mutation;

import graphql.schema.DataFetchingEnvironment;

public interface RelayMutation<I extends RelayMutationType, R extends RelayMutationType> {

    R call(I input, DataFetchingEnvironment env);
}
