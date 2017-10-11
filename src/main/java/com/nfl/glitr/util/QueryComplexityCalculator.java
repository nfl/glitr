package com.nfl.glitr.util;

import com.nfl.glitr.exception.GlitrException;
import graphql.language.*;
import graphql.parser.Parser;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
    private final String MUTATION_DEFINITION = "mutation";

    private final int maxCharacterLimit;
    private final int maxDepthLimit;
    private final int maxScoreLimit;
    private final int defaultMultiplier;
    private final Parser documentParser;
    private Map<String, Integer> queryComplexityMultipliersMap = new HashMap<>();
    private Set<String> queryComplexityExcludeNodes = new HashSet<>();

    public QueryComplexityCalculator() {
        this.maxCharacterLimit = 10000;
        this.maxDepthLimit = 8;
        this.maxScoreLimit = 500;
        this.defaultMultiplier = 10;
        this.documentParser = new Parser();
    }

    public QueryComplexityCalculator(int maxCharacterLimit, int maxDepthLimit, int maxScoreLimit, int defaultMultiplier) {
        this.maxCharacterLimit = maxCharacterLimit;
        this.maxDepthLimit = maxDepthLimit;
        this.maxScoreLimit = maxScoreLimit;
        this.defaultMultiplier = defaultMultiplier;
        this.documentParser = new Parser();
    }

    public QueryComplexityCalculator(int maxCharacterLimit, int maxDepthLimit, int maxScoreLimit, int defaultMultiplier, Parser documentParser) {
        this.maxCharacterLimit = maxCharacterLimit;
        this.maxDepthLimit = maxDepthLimit;
        this.maxScoreLimit = maxScoreLimit;
        this.defaultMultiplier = defaultMultiplier;
        this.documentParser = documentParser;
    }

    public QueryComplexityCalculator withQueryComplexityExcludeNodes(Set<String> queryComplexityExcludeNodes) {
        this.queryComplexityExcludeNodes = Optional.ofNullable(queryComplexityExcludeNodes).orElse(new HashSet<>());
        return this;
    }

    public QueryComplexityCalculator withQueryComplexityMultipliersMap(Map<String, Integer> queryComplexityMultipliersMap) {
        this.queryComplexityMultipliersMap = Optional.ofNullable(queryComplexityMultipliersMap).orElse(new HashMap<>());
        return this;
    }

    /**
     * @param query - graphql query string
     * We want to validate the query or fail fast.
     */
    public void validate(String query) {
        if (characterLimitExceeded(query)) {
            throw new GlitrException(String.format("query length has exceeded the maximum of %d characters.", maxCharacterLimit));
        }

        if (depthLimitExceeded(query)) {
            throw new GlitrException(String.format("query depth has exceeded the maximum depth level of %d.", maxDepthLimit));
        }

        if (scoreLimitExceeded(query)){
            throw new GlitrException(String.format("query score has exceeded the maximum score level of %d.", maxScoreLimit));
        }

    }

    /**
     * @param query - graphql query string
     * @return true if the query's length is greater than the maximum allowed number of characters.
     */
    public boolean characterLimitExceeded(String query) {
        return characterScore(query) > maxCharacterLimit;
    }

    /**
     * @param query - graphql query string
     * @return the length of the query string.  If the query is a mutation, we retrieve the return query body of the mutation
     */
    public int characterScore(String query) {
        if (StringUtils.isBlank(query)) {
            throw new GlitrException("query cannot be null or empty");
        }

        if (isMutation(query)){
            query = extractReturnQueryFromMutation(query);
        }

        return query.trim().length();
    }

    /**
     * @param query - graphql query string
     * @return true if the query's depth is greater than the maximum allowed depth.
     */
    public boolean depthLimitExceeded(String query) {
        return depthScore(query) > maxDepthLimit;
    }

    /**
     * @param query - graphql query string
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

     /* @param query = query string
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

    /**
     * @param query - graphql query string
     * @return true if the query's score is greater than the maximum allowed score.
     */
    public boolean scoreLimitExceeded(String query) {
        return queryScore(query) > maxScoreLimit;
    }

    /**
     * @param query - graphql query string
     * @return query score
     */
    public int queryScore(String query) {
        String parent = null;
        if (isMutation(query)) {
            parent = getMutationName(query);
        }

        return queryScore(parent, parseRootNode(query), 0);
    }

    private String getMutationName(String query) {
        return trimToNull(substringBetween(query, "{", "("));
    }

    private int queryScore(String path, Node queryNode, int depth) {
        int score = 0;
        int multiplier = 0;

        String nodeType = queryNode.getClass().getSimpleName().toUpperCase();
        if (nodeType.equals(QUERY_FIELD) && !isLeaf(queryNode)) {
            Field field = (Field) queryNode;
            if (!queryComplexityExcludeNodes.contains(NodeUtil.buildPath(path, field.getName()))) {
                path = NodeUtil.buildPath(path, field.getName());
                multiplier = extractMultiplierFromListField(field, path);
                depth += 1;
            }
        }

        for (Node currentChild : queryNode.getChildren()) {
            if (currentChild.getClass() == Argument.class) {
                continue;
            }
            score += (depth * multiplier) + queryScore(path, currentChild, depth);
        }
        return score;
    }

    /**
     *
     * @param query - graphql query string
     * @return the root node.  We parse the query with a graphql library and retrieve a document object.  We filter the
     * document object in search of an OPERATION_DEFINITION.  If the query is a MUTATION we extract the return query
     * from the mutation body.  If it's not a mutation, we parse as is.
     */
    private OperationDefinition parseRootNode(String query) {
        if (isMutation(query)) {
            query = extractReturnQueryFromMutation(query);
        }

        Document document;
        try {
            document = documentParser.parseDocument(query);
        } catch (Exception e) {
            throw new GlitrException(String.format("Cannot parse query %s", query));
        }

        Optional<OperationDefinition> baseNode = document.getChildren().stream()
                .filter(n -> (n.getClass() == OperationDefinition.class))
                .map(node -> (OperationDefinition) node)
                .findFirst();

        if (!baseNode.isPresent()) {
            throw new GlitrException("Cannot find node 'OperationDefinition'");
        }

        return baseNode.get();
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
    protected int extractMultiplierFromListField(Field node, String path) {
        Optional<Argument> listLimit = node.getArguments().stream()
                .filter(argument -> argument.getName().equals("first"))
                .findFirst();

        // If there is an argument
        if (listLimit.isPresent()) {
            return ((IntValue) listLimit.get().getValue()).getValue().intValue();
        }

        // If it has children with no argument
        if (!isLeaf(node)) {
            return queryComplexityMultipliersMap.getOrDefault(path, defaultMultiplier);
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

    public boolean isMutation(String query){
        return query.trim().startsWith(MUTATION_DEFINITION);
    }

    /**
     * @param query - graphql query string
     * @return the query that is returned by the mutation response body
     */
    private String extractReturnQueryFromMutation(String query){
        //We need to find the end of the mutation input, which is '})'
        int endOfMutationInput = query.indexOf("})");

        if (endOfMutationInput == -1){
            throw new GlitrException("Malformed mutation query");
        }

        // We return the string after the index and we also remove the last } which is part of the entire mutation query which we don't need.
        return query.substring(endOfMutationInput + 2, query.lastIndexOf("}")).trim();

    }

    public int getMaxCharacterLimit() {
        return maxCharacterLimit;
    }

    public int getMaxDepthLimit() {
        return maxDepthLimit;
    }

    public int getMaxScoreLimit() {
        return maxScoreLimit;
    }

    public int getDefaultMultiplier() {
        return defaultMultiplier;
    }
}

