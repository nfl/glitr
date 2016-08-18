package com.nfl.dm.shield.graphql.relay;

import graphql.schema.*;
import java.util.List;

public interface Relay {

    GraphQLInterfaceType nodeInterface(TypeResolver typeResolver);
    GraphQLFieldDefinition nodeField(GraphQLInterfaceType nodeInterface, DataFetcher nodeDataFetcher);

    List<GraphQLArgument> getConnectionFieldArguments();
    List<GraphQLArgument> getBackwardPaginationConnectionFieldArguments();
    List<GraphQLArgument> getForwardPaginationConnectionFieldArguments();

    GraphQLObjectType edgeType(String name,
                               GraphQLOutputType nodeType,
                               GraphQLInterfaceType nodeInterface,
                               List<GraphQLFieldDefinition> edgeFields);
    GraphQLObjectType connectionType(String name,
                                     GraphQLObjectType edgeType,
                                     List<GraphQLFieldDefinition> connectionFields);

    String toGlobalId(String type, String id);
    graphql.relay.Relay.ResolvedGlobalId fromGlobalId(String globalId);

    GraphQLFieldDefinition mutationWithClientMutationId(String name, String fieldName,
                                                        List<GraphQLInputObjectField> inputFields,
                                                        List<GraphQLFieldDefinition> outputFields,
                                                        DataFetcher dataFetcher);

    class ResolvedGlobalId {
        public ResolvedGlobalId(String type, String id) {
            this.type = type;
            this.id = id;
        }

        public String type;
        public String id;
    }
}
