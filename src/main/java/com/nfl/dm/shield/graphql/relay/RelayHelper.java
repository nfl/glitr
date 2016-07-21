package com.nfl.dm.shield.graphql.relay;

import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.NodeFetcherService;
import graphql.relay.*;
import graphql.schema.*;

import java.util.ArrayList;
import java.util.List;

import static graphql.Assert.assertNotNull;

public class RelayHelper {

    private Relay relay;
    private static final String DUMMY_CURSOR_PREFIX = "simple-cursor";

    private final TypeRegistry typeRegistry;
    private GraphQLFieldDefinition nodeField;


    public RelayHelper(Relay relay, TypeRegistry typeRegistry, NodeFetcherService nodeFetcherService) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(typeRegistry.getNodeInterface(), "NodeInterface can't be null");
        this.relay = relay;
        this.typeRegistry = typeRegistry;
        this.nodeField = relay.nodeField(typeRegistry.getNodeInterface(), nodeFetcherService);
    }

    public GraphQLInterfaceType getNodeInterface() {
        return typeRegistry.getNodeInterface();
    }

    public GraphQLFieldDefinition getNodeField() {
        return nodeField;
    }

    public List<GraphQLArgument> getConnectionFieldArguments() {
        return relay.getConnectionFieldArguments();
    }

    public GraphQLObjectType edgeType(String simpleName, GraphQLOutputType edgeGraphQLOutputType,
                                             GraphQLInterfaceType nodeInterface, List<GraphQLFieldDefinition> graphQLFieldDefinitions) {
        return relay.edgeType(simpleName, edgeGraphQLOutputType, nodeInterface, graphQLFieldDefinitions);
    }

    public GraphQLObjectType connectionType(String simpleName, GraphQLObjectType edgeType, List<GraphQLFieldDefinition> graphQLFieldDefinitions) {
        return relay.connectionType(simpleName, edgeType, graphQLFieldDefinitions);
    }

    public static graphql.relay.Connection buildConnection(Iterable<?> col, int skipItems, int totalCount) {
        List<Edge> edges = new ArrayList<>();
        int ix = skipItems;

        for (Object object : col) {
            edges.add(new Edge(object, new ConnectionCursor(createCursor(ix++))));
        }

        PageInfo pageInfo = new PageInfo();

        if (edges.size() > 0) {
            Edge firstEdge = edges.get(0);
            Edge lastEdge = edges.get(edges.size() - 1);
            pageInfo.setStartCursor(firstEdge.getCursor());
            pageInfo.setEndCursor(lastEdge.getCursor());
        }

        pageInfo.setHasPreviousPage(skipItems > 0 && totalCount > 0);
        pageInfo.setHasNextPage(skipItems + edges.size() + 1 < totalCount);

        graphql.relay.Connection connection = new graphql.relay.Connection();
        connection.setEdges(edges);
        connection.setPageInfo(pageInfo);
        return connection;
    }

    public static String createCursor(int offset) {
        return Base64.toBase64(DUMMY_CURSOR_PREFIX + Integer.toString(offset));
    }

    public static int getOffsetFromCursor(String cursor, int defaultValue) {
        if (cursor == null) return defaultValue;
        String string = Base64.fromBase64(cursor);
        return Integer.parseInt(string.substring(DUMMY_CURSOR_PREFIX.length()));
    }
}
