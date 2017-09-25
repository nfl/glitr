package com.nfl.glitr.relay;

import com.nfl.glitr.exception.GlitrException;
import com.nfl.glitr.registry.TypeRegistry;
import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultConnection;
import graphql.relay.DefaultConnectionCursor;
import graphql.relay.DefaultEdge;
import graphql.relay.Edge;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static graphql.Assert.assertNotNull;
import static java.lang.String.format;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

public class RelayHelper {

    private Relay relay;
    private static final String DUMMY_CURSOR_PREFIX = "simple-cursor";

    private final TypeRegistry typeRegistry;


    public RelayHelper(Relay relay, TypeRegistry typeRegistry) {
        assertNotNull(typeRegistry, "TypeRegistry can't be null");
        assertNotNull(typeRegistry.getNodeInterface(), "NodeInterface can't be null");
        this.relay = relay;
        this.typeRegistry = typeRegistry;
    }

    public GraphQLInterfaceType getNodeInterface() {
        return typeRegistry.getNodeInterface();
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
        List<Edge<Object>> edges = new ArrayList<>();
        int ix = skipItems;

        for (Object object : col) {
            edges.add(new DefaultEdge<>(object, new DefaultConnectionCursor(createCursor(ix++))));
        }

        ConnectionCursor startCursor = null;
        ConnectionCursor endCursor = null ;
        boolean hasPreviousPage = skipItems > 0 && totalCount > 0;
        boolean hasNextPage = skipItems + edges.size() + 1 < totalCount;

        if (edges.size() > 0) {
            Edge firstEdge = edges.get(0);
            Edge lastEdge = edges.get(edges.size() - 1);
            startCursor = firstEdge.getCursor();
            endCursor = lastEdge.getCursor();
        }

        PageInfoWithTotal pageInfoWithTotal = new PageInfoWithTotal(startCursor, endCursor,
                hasPreviousPage, hasNextPage);
        pageInfoWithTotal.setTotal(totalCount);

        return new DefaultConnection<>(edges, pageInfoWithTotal);
    }

    public static String createCursor(int offset) {
        byte[] bytes = (DUMMY_CURSOR_PREFIX + Integer.toString(offset)).getBytes(StandardCharsets.UTF_8);
        return getEncoder().encodeToString(bytes);
    }

    public static int getOffsetFromCursor(String cursor, int defaultValue) {
        if (cursor == null) {
            return defaultValue;
        }
        byte[] decode;
        try {
            decode = getDecoder().decode(cursor);
        } catch (IllegalArgumentException e) {
            throw new GlitrException(format("The cursor is not in base64 format : '%s'", cursor), e);
        }
        String string = new String(decode, StandardCharsets.UTF_8);
        if (DUMMY_CURSOR_PREFIX.length() > string.length()) {
            throw new GlitrException(format("The cursor prefix is missing from the cursor : '%s'", cursor));
        }
        try {
            return Integer.parseInt(string.substring(DUMMY_CURSOR_PREFIX.length()));
        } catch (NumberFormatException nfe) {
            throw new GlitrException(format("The cursor was not created by this class  : '%s'", cursor), nfe);
        }
    }
}
