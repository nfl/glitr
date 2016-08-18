package com.nfl.dm.shield.graphql.relay;

import graphql.schema.*;

import java.util.List;

import static graphql.Scalars.*;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class RelayImpl extends graphql.relay.Relay implements Relay {

    private GraphQLObjectType pageInfoType = newObject()
            .name("PageInfoWithTotal")
            .description("Information about pagination in a connection.")
            .field(newFieldDefinition()
                    .name("hasNextPage")
                    .type(new GraphQLNonNull(GraphQLBoolean))
                    .description("When paginating forwards, are there more items?")
                    .build())
            .field(newFieldDefinition()
                    .name("hasPreviousPage")
                    .type(new GraphQLNonNull(GraphQLBoolean))
                    .description("When paginating backwards, are there more items?")
                    .build())
            .field(newFieldDefinition()
                    .name("startCursor")
                    .type(GraphQLString)
                    .description("When paginating backwards, the cursor to continue.")
                    .build())
            .field(newFieldDefinition()
                    .name("endCursor")
                    .type(GraphQLString)
                    .description("When paginating forwards, the cursor to continue.")
                    .build())
            .field(newFieldDefinition()
                    .name("total")
                    .type(GraphQLBigInteger)
                    .description("Total number of elements in the connection.")
                    .build())
            .build();

    @Override
    public GraphQLObjectType connectionType(String name, GraphQLObjectType edgeType, List<GraphQLFieldDefinition> connectionFields) {
        return newObject()
                .name(name + "Connection")
                .description("A connection to a list of items.")
                .field(newFieldDefinition()
                        .name("edges")
                        .type(new GraphQLList(edgeType))
                        .build())
                .field(newFieldDefinition()
                        .name("pageInfo")
                        .type(new GraphQLNonNull(pageInfoType))
                        .build())
                .fields(connectionFields)
                .build();
    }
}
