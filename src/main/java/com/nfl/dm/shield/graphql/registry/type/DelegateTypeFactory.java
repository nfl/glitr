package com.nfl.dm.shield.graphql.registry.type;

import graphql.schema.GraphQLType;

/**
 * Factory for the creation of GraphQL type objects and are delegates of {@link GraphQLTypeFactory}
 */
@FunctionalInterface
public interface DelegateTypeFactory {

    GraphQLType create(Class clazz);
}
