package com.nfl.glitr.util.complexity;

import com.nfl.glitr.exception.GlitrException;
import graphql.language.*;
import graphql.parser.Parser;
import org.apache.commons.lang3.StringUtils;
import java.util.Optional;

/*
     This class is used as a tool to compute complexity of a graphql string query.
     We can test the query's character limit.
     We can test the query's depth limit.
     We can test the query's overall score.

     * maxCharacterLimit = maximum allowed characters in the query string.
     * maxDepthLimit = maximum allowed depth in the query string.
     * defaultMultiplier = multiplier per depth level
 */

public class QueryComplexityCalculator {

    private final String QUERY_FIELD = "FIELD";

    private final int maxCharacterLimit;
    private final int maxDepthLimit;
    private final int defaultMultiplier;
    private final Parser documentParser;

    public QueryComplexityCalculator() {
        this.maxCharacterLimit = 5;
        this.maxDepthLimit = 5;
        this.defaultMultiplier = 10;
        this.documentParser = new Parser();
    }

    public QueryComplexityCalculator(int maxCharacterLimit, int maxDepthLimit, int defaultMultiplier) {
        this.maxCharacterLimit = maxCharacterLimit;
        this.maxDepthLimit = maxDepthLimit;
        this.defaultMultiplier = defaultMultiplier;
        this.documentParser = new Parser();
    }

    public QueryComplexityCalculator(int maxCharacterLimit, int maxDepthLimit, int defaultMultiplier, Parser documentParser) {
        this.maxCharacterLimit = maxCharacterLimit;
        this.maxDepthLimit = maxDepthLimit;
        this.defaultMultiplier = defaultMultiplier;
        this.documentParser = documentParser;
    }

    /**
     * @param query - Query string to test against
     * @return the query score after we have verified the character / depth limits are adequate.
     */
    public int checkAndRetrieveQueryScore(String query) {
        if (characterLimitExceeded(query)) {
            throw new GlitrException(String.format("query length has exceeded the maximum of %d characters.", maxCharacterLimit));
        }

        if (depthLimitExceeded(query)) {
            throw new GlitrException(String.format("query depth has exceeded the maximum depth level of %d.", maxDepthLimit));
        }

        return queryScore(query);
    }

    /**
     * @param query - query string
     * @return true if the query's length is greater than the maximum allowed number of characters.
     */
    public boolean characterLimitExceeded(String query) {
        return characterScore(query) > maxCharacterLimit;
    }

    /**
     * @param query - query string
     * @return the length of the query string.
     */
    public int characterScore(String query) {
        if (StringUtils.isBlank(query)) {
            throw new GlitrException("query cannot be null or empty");
        }

        return query.trim().length();
    }

    /**
     * @param query - query string
     * @return true if the query's depth is greater than the maximum allowed depth.
     */
    public boolean depthLimitExceeded(String query) {
        return depthScore(query) > maxDepthLimit;
    }

    /**
     * @param query - query string
     * @return the maximum depth of the query string
     */
    public int depthScore(String query) {
        if (StringUtils.isBlank(query)) {
            throw new GlitrException("query cannot be null or empty");
        }

        return calculateMaxDepth(parseRootNode(query.trim()));
    }

    /**
     * @param queryNode - Root node to recursively iterate and find the deepest child.
     * @return the maximum child depth as an integer.  We start at the queryNode.  We figure out what type of node this is.
     * It can be an OPERATION_DEFINITION, a SELECTION_SET, a FIELD, etc.  If it's a field and not a LEAF, we increase
     * the depth by 1.  A node is considered a LEAF when it doesn't have any children.  We continue and recursively go
     * through each child and figure out what the deepest one is.  The deepest child is what is returned as the maximum depth.
     *
     * Examples
     ******************************************************************************************************************
     ******************************************************************************************************************
     * Depth = 0
     * {
     *     trackName
     * }
     ******************************************************************************************************************
     ******************************************************************************************************************
     * Depth = 1
     * {
     *     playLists{
     *         playListId
     *     }
     * }
     ******************************************************************************************************************
     ******************************************************************************************************************
     * Depth = 2
     * {
     *     playLists{
     *         playListId
     *         tracks{
     *             trackId
     *         }
     *     }
     * }
     ******************************************************************************************************************
     ******************************************************************************************************************
     * Depth = 2
     * {
     *     playLists{
     *         playListId
     *         tracks{
     *             trackId
     *         }
     *         artists{
     *             artistId
     *         }
     *     }
     * }
     ******************************************************************************************************************
     ******************************************************************************************************************
     * Depth = 3
     * {
     *     playLists{
     *         playListId
     *         tracks{
     *             trackId
     *             trackInfo{
     *                 runtime
     *             }
     *         }
     *     }
     * }
     ******************************************************************************************************************
     ******************************************************************************************************************
     */
    private int calculateMaxDepth(Node queryNode) {
        int depth = 0;

        String nodeType = queryNode.getClass().getSimpleName().toUpperCase();
        if (nodeType.equals(QUERY_FIELD) && !isLeaf(queryNode)) {
            depth = 1;
        }

        int maxChildDepth = 0;
        for (Node childNode : queryNode.getChildren()) {
            final int currentChildDepth = calculateMaxDepth(childNode);
            maxChildDepth = Math.max(maxChildDepth, currentChildDepth);
        }

        depth += maxChildDepth;
        return depth;
    }

    /**
     * @param query = query string
     * @return query score as an integer.  The way the query score is calculated is by summing the depth * multiplier of
     * all non-leaf nodes.
     ******************************************************************************************************************
     ******************************************************************************************************************
     * {
     *     playLists{    depth = 1, multiplier = 10 (default)
     *         id
     *     }
     * }
     * score = (1 x 10) = 10
     ****************************************************************************************************************
     ****************************************************************************************************************
     * {
     *     playLists{               depth = 1, multiplier = 10
     *         id
     *         tracks(first:4){     depth = 2, multiplier = 4
     *             trackId
     *         }
     *     }
     * }
     * score = (1 * 10) + (2 * 4) = 18
     ****************************************************************************************************************
     ****************************************************************************************************************
     * {
     *     playLists{               depth = 1, multiplier = 10
     *         id
     *         tracks(first:4){     depth = 2, multiplier = 4
     *             trackId
     *             trackInfo{       depth = 3, multiplier = 10
     *                 runtime
     *             }
     *         }
     *     }
     * }
     * score = (1 * 10) + (2 * 4) + (3 * 10) = 48
     ****************************************************************************************************************
     ****************************************************************************************************************
     *{
     *     playLists{                      depth = 1, multiplier = 10
     *         id
     *         tracks(first:4){            depth = 2, multiplier = 4
     *             trackId
     *             trackInfo{              depth = 3, multiplier = 10
     *                 runtime
     *                 authors(first:2){   depth = 4, multiplier = 2
     *                     authorName
     *                 }
     *             }
     *         }
     *     }
     * }
     * score = (1 * 10) + (2 * 4) + (3 * 10) + (4 * 2) = 56
     ******************************************************************************************************************
     ******************************************************************************************************************
     */
    public int queryScore(String query) {
        return queryScore(parseRootNode(query), 0);
    }

    private int queryScore(Node queryNode, int depth) {
        int score = 0;
        int multiplier = 0;

        String nodeType = queryNode.getClass().getSimpleName().toUpperCase();
        if (nodeType.equals(QUERY_FIELD) && !isLeaf(queryNode)) {
            Field field = (Field) queryNode;
            multiplier = extractMultiplierFromListField(field);
            depth += 1;
        }

        for (Node currentChild : queryNode.getChildren()) {
            if (currentChild.getClass() == Argument.class) {
                continue;
            }
            score += (depth * multiplier) + queryScore(currentChild, depth);
        }
        return score;
    }

    /**
     *
     * @param query - query string
     * @return the root node.  We parse the query with a graphql library and retrieve a document object.  We filter the
     * document object in search of an OPERATION_DEFINITION and make sure its a QUERY and not a MUTATION.  Once we find
     * the object, we return it.
     */
    private Node parseRootNode(String query) {
        Document document;
        try {
            document = documentParser.parseDocument(query);
        } catch (Exception e) {
            throw new GlitrException(String.format("Cannot parse query %s", query));
        }

        Optional<Node> queryNode = document.getChildren().stream()
                .filter(n -> (n.getClass() == OperationDefinition.class))
                .filter(n -> ((OperationDefinition)n).getOperation() == OperationDefinition.Operation.QUERY)
                .findFirst();

        if (!queryNode.isPresent()) {
            throw new GlitrException("Cannot find query node 'OperationDefinition' or query is a 'MUTATION'");
        }

        return queryNode.get();

    }

    /**
     *
     * @param node - The field node we would like to test against
     * @return the multiplier
     *
     ****************************************************************************************************************
     ****************************************************************************************************************
     * If the node has an argument called 'first' and has children, we return that integer as the multiplier
     * {
     *     players(first:5){
     *         playerId
     *     }
     * } --> returns 5
     *****************************************************************************************************************
     *****************************************************************************************************************
     *
     *****************************************************************************************************************
     *****************************************************************************************************************
     * If the node doesn't have an argument but has children, we return the default multiplier
     * {
     *     players{
     *         playerId
     *     }
     * } --> returns 10, which is the default multiplier above.
     *****************************************************************************************************************
     *****************************************************************************************************************
     *
     *****************************************************************************************************************
     *****************************************************************************************************************
     * If the node is a leaf node, we return 1.
     * {
     *     playerName
     * }
     *****************************************************************************************************************
     *****************************************************************************************************************
     */
    private int extractMultiplierFromListField(Field node) {
        Optional<Argument> listLimit = node.getArguments().stream()
                .filter(argument -> argument.getName().equals("first"))
                .findFirst();

        // If there is an argument
        if (listLimit.isPresent()) {
            return ((IntValue) listLimit.get().getValue()).getValue().intValue();
        }

        // If it has children with no argument
        if (!isLeaf(node)) {
            return defaultMultiplier;
        }

        // If its a leaf
        return 1;
    }

    /**
     * @param node - Node to test against
     * @return  true  --> if the node doesn't have children
     *          false --> if the node has children.
     */
    private boolean isLeaf(Node node) {
        return node.getChildren().isEmpty();
    }
}

