package com.nfl.glitr.relay;

import com.nfl.glitr.registry.TypeRegistry;
import graphql.relay.*;
import graphql.schema.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static graphql.Assert.assertNotNull;

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

    /**
     *
     * @param col - items to be returned
     * @param offset - identifier of the starting point to return items from a result set
     * @param itemsPerPage - the limit of items that should be returned per request
     * @param totalCount - total amount of items
     * @return {@link graphql.relay.Connection}
     */
    public static graphql.relay.Connection buildConnection(Iterable<?> col, int offset, int itemsPerPage, int totalCount) {
        List<Edge<Object>> edges = new ArrayList<>();
        int ix = offset;

        for (Object object : col) {
            edges.add(new DefaultEdge<>(object, new DefaultConnectionCursor(createCursor(ix++))));
        }

        ConnectionCursor startCursor = null;
        ConnectionCursor endCursor = null ;
        ConnectionCursor previousPageStartCursor = null ;

        boolean hasPreviousPage = offset > 0 && totalCount > 0;
        boolean hasNextPage = offset + edges.size() + 1 < totalCount;

        if (!edges.isEmpty()) {
            Edge firstEdge = edges.get(0);
            Edge lastEdge = edges.get(edges.size() - 1);
            startCursor = firstEdge.getCursor();
            endCursor = lastEdge.getCursor();

            if (offset - itemsPerPage > 0) {
                String cursor = createCursor(offset - itemsPerPage - 1);
                previousPageStartCursor = new DefaultConnectionCursor(cursor);
            }
        }

        PageInfoWithTotal pageInfoWithTotal = new PageInfoWithTotal(startCursor, endCursor, hasPreviousPage, hasNextPage);
        pageInfoWithTotal.setTotal(totalCount);
        pageInfoWithTotal.setPreviousPageStartCursor(previousPageStartCursor);

        return new DefaultConnection<>(edges, pageInfoWithTotal);
    }

    public static String createCursor(int offset) {
        return Base64Helper.toBase64(DUMMY_CURSOR_PREFIX + offset);
    }

    public static int getOffsetFromCursor(String cursor, int defaultValue) {
        if (cursor == null) return defaultValue;
        String string = Base64Helper.fromBase64(cursor);
        return Integer.parseInt(string.substring(DUMMY_CURSOR_PREFIX.length()));
    }


    static public class Base64Helper {

        private Base64Helper() {
        }

        public static String toBase64(String string) {
            return Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
        }

        public static String fromBase64(String string) {
            return new String(Base64.getDecoder().decode(string), Charset.forName("UTF-8"));
        }
    }
}
