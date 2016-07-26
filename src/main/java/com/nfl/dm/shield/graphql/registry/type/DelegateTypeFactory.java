package com.nfl.dm.shield.graphql.registry.type;

import graphql.schema.GraphQLType;

@FunctionalInterface
public interface DelegateTypeFactory {

    GraphQLType create(Class clazz);
}
