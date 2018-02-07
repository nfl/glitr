package com.nfl.glitr.registry.schema;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLType;

public class GraphQLConnectionList extends GraphQLList {
    public GraphQLConnectionList(GraphQLType wrappedType) {
        super(wrappedType);
    }
}
