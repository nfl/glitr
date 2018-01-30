package com.nfl.glitr.util;

import com.nfl.glitr.exception.GlitrException;
import graphql.language.*;
import graphql.parser.Parser;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Logger logger = LoggerFactory.getLogger(QueryComplexityCalculator.class);

    private static final String[] ALPHABET = new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
    private static final Pattern FORMULA_VARIABLE_PATTERN = Pattern.compile("(#\\{.*?})");
    private static final String FORMULA_VARIABLE_OPEN_TAG = "#{";
    private static final String FORMULA_VARIABLE_CLOSS_TAG = "}";

    private final String QUERY_FIELD = "FIELD";
    private final String MUTATION_DEFINITION = "mutation";

    private final int maxCharacterLimit;
    private final int maxDepthLimit;
    private final int maxScoreLimit;
    private final double defaultMultiplier;
    private final Parser documentParser;
    private Map<String, String> queryComplexityMultipliersMap = new HashMap<>();
    private Set<String> queryComplexityExcludeNodes = new HashSet<>();

    public QueryComplexityCalculator() {
        this.maxCharacterLimit = 10000;
        this.maxDepthLimit = 8;
        this.maxScoreLimit = 500;
        this.defaultMultiplier = 10d;
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

    public QueryComplexityCalculator withQueryComplexityMultipliersMap(Map<String, String> queryComplexityMultipliersMap) {
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
        for (Node childNode : (List<Node>) queryNode.getChildren()) {
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
    * <pre>
    *
    * ****************************************************************************************************************
    * ****************************************************************************************************************
    * {
    *     playLists{    multiplier = 10 (default)
    *         id
    *     }
    * }
    * score = 10
    * ****************************************************************************************************************
    * ****************************************************************************************************************
    * {
    *     playLists{               multiplier (default) = 10
    *         id
    *         tracks(first:4){     first = 4, multiplier (default) = 10
    *              trackId
    *         }
    *     }
    * }
    * score = (10) + (4 * 10) = 50
    * ****************************************************************************************************************
    * ****************************************************************************************************************
    * {
    *     playLists{                      multiplier (default) = 10
    *         id
    *         tracks(first:4){            first = 4, multiplier (default) = 10
    *             trackId
    *             trackInfo{              multiplier (default) = 10
    *                 runtime
    *                 authors(first:2){   first = 2, multiplier (default) = 10
    *                     authorName
    *                 }
    *             }
    *         }
    *     }
    * }
    * score = 10 + (4 * 10) + 10 + (2 * 10) = 70
    * ****************************************************************************************************************
    * ****************************************************************************************************************
    * {
    *     playLists{               multiplier (default) = 10
    *         id
    *         tracks(first:4){     first = 4, multiplier (default) = 10
    *             trackId
    *             trackInfo{       multiplier (default) = 10
    *                 runtime
    *             }
    *         }
    *     }
    * }
    * score = 10 + (4 * 10) + 10 = 60
    * ****************************************************************************************************************
    * ****************************************************************************************************************
    * {
    *     playLists{               multiplier (default) = 10
    *         id
    *         tracks(first:4){     first = 4, multiplier (default) = 10
    *             trackId
    *             trackInfo{       multiplier (default) = 10
    *                 runtime
    *             }
    *         }
    *     }
    * }
    * score = 10 + (4 * 10) + 10 = 60
    * ****************************************************************************************************************
    * ****************************************************************************************************************
    * {@literal @}GlitrQueryComplexity("#{depth} + #{childScore} + #{currentCollectionSize} + #{totalCollectionsSize}
    *                                  + #{maxCharacterLimit} + #{maxDepthLimit} + #{maxScoreLimit} + #{defaultMultiplier} + 5")
    *  private List trackInfo;
    *
    *  #{maxCharacterLimit} = 11, #{maxDepthLimit} = 22, #{maxScoreLimit} = 33, #{defaultMultiplier} = 10
    *
    * {
    *     playLists{                      multiplier (default) = 10
    *         id
    *         tracks(first:4){            first = 4, multiplier (default) = 10
    *             trackId
    *             trackInfo(first:2){     #{depth} = 3, #{childScore} = 10, #{currentCollectionSize} = 2, #{totalCollectionsSize} = 6
    *                 runtime
    *                 author{             multiplier (default) = 10
    *                     authorName
    *                 }
    *             }
    *         }
    *     }
    * }
    * score = 10 + (4 * 10) + (3 + 10 + 2 + 6 + 11 + 22 + 33 + 10 + 5) + 10 = 162
    * ****************************************************************************************************************
    * ****************************************************************************************************************
    * </pre>
    * @param query string
    * @return query score as an double.  The way the query score is calculated is by summing the multipliers of all non-leaf nodes.
    **/
    public double queryScore(String query) {
        String parent = null;
        if (isMutation(query)) {
            parent = getMutationName(query);
            System.out.println("mutation name = " + parent);
        }

        return queryScore(parent, parseRootNode(query), 0, new HashMap<>());
    }

    private String getMutationName(String query) {
        return trimToNull(substringBetween(query, "{", "("));
    }

    @SuppressWarnings("unchecked")
    private double queryScore(String path, Node queryNode, int depth, Map<String, Double> queryContext) {
        double currentNodeScore = 0d;
        double childScores = 0d;

        if (isAccessibleField(queryNode, path)) {
            depth++;
            path = NodeUtil.buildNewPath(path, ((Field) queryNode).getName());
            refreshQueryContext(queryContext, (Field) queryNode);
        }

        for (Node currentChild : (List<Node>) queryNode.getChildren()) {
            if (currentChild.getClass() == Argument.class) {
                continue;
            }

            childScores += queryScore(path, currentChild, depth, queryContext);
        }

        if (isAccessibleField(queryNode, path) ) {
            Map<String, Double> multiplierContext = buildContext((Field) queryNode, queryContext, depth, childScores);
            currentNodeScore = extractMultiplierFromListField((Field) queryNode, path, multiplierContext);
        }

        return currentNodeScore + childScores;
    }

    private void refreshQueryContext(Map<String, Double> queryContext, Field field) {
        int limit = getLimitArgIfPresent(field).orElse(0);

        Double parentCollectionsSize = queryContext.getOrDefault("totalCollectionsSize", 0d);
        Double totalCollectionsSize = parentCollectionsSize + limit;
        queryContext.put("totalCollectionsSize", totalCollectionsSize);
    }

    private Map buildContext(Field field, Map<String, Double> queryContext, int depth, double childScores) {
        double totalCollectionsSize = queryContext.getOrDefault("totalCollectionsSize", 0d);
        int limit = getLimitArgIfPresent(field).orElse(0);

        Map<String, Double> multiplierContext = new HashMap<>();
        multiplierContext.put("totalCollectionsSize", totalCollectionsSize);
        multiplierContext.put("currentCollectionSize", (double) limit);
        multiplierContext.put("depth", (double) depth);
        multiplierContext.put("childScore", childScores);

        multiplierContext.put("maxCharacterLimit", (double) maxCharacterLimit);
        multiplierContext.put("maxDepthLimit", (double) maxDepthLimit);
        multiplierContext.put("maxScoreLimit", (double) maxScoreLimit);
        multiplierContext.put("defaultMultiplier", defaultMultiplier);

        return multiplierContext;
    }

    private boolean isAccessibleField(Node node, String path) {
        if (node == null) {
            return false;
        }

        if (!(node instanceof Field)) {
            return false;
        }

        path = NodeUtil.buildNewPath(path, ((Field) node).getName());
        return !queryComplexityExcludeNodes.contains(path) && !isLeaf(node);
    }

    private Optional<Integer> getLimitArgIfPresent(Field node) {
        Integer limit = null;

        Optional<Argument> listLimit = node.getArguments().stream()
                .filter(argument -> argument.getName().equals("first"))
                .findFirst();

        if (listLimit.isPresent()) {
            limit = ((IntValue) listLimit.get().getValue()).getValue().intValue();
        }

        return Optional.ofNullable(limit);
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
     * <pre>
     * ****************************************************************************************************************
     * ****************************************************************************************************************
     * If the node has an argument called 'first' and has children, we return that integer multiplied by the default multiplier
     * default multiplier = 10
     * {
     *     players(first:5){
     *         playerId
     *     }
     * } == returns 5 * 10 = 50
     * ****************************************************************************************************************
     * ****************************************************************************************************************
     * If the node has an argument called 'first' and annotated by @GlitrQueryComplexity, at the same time - we ignore the 'first' argument and return specified complexity value
     *
     * {@literal @}GlitrQueryComplexity("70")
     *  private List players;
     *
     * {
     *     players(first:5){
     *         playerId
     *     }
     * } == returns 70
     * *****************************************************************************************************************
     * *****************************************************************************************************************
     *
     * *****************************************************************************************************************
     * *****************************************************************************************************************
     * If the node doesn't have an argument but has children, we return the default multiplier
     * {
     *     players{
     *         playerId
     *     }
     * } == returns 10, which is the default multiplier above.
     * *****************************************************************************************************************
     * *****************************************************************************************************************
     *
     * *****************************************************************************************************************
     * *****************************************************************************************************************
     * If the node is a leaf node, we return 1.
     * {
     *     playerName
     * }
     * *****************************************************************************************************************
     * *****************************************************************************************************************
     * </pre>
     *
     * @param node - The field node we would like to test against
     * @param path - Already tested nodes chain
     * @param context - mutable global query context
     * @return the multiplier
     *
     **/
    protected double extractMultiplierFromListField(Field node, String path, Map<String, Double> context) {
        String multiplier = queryComplexityMultipliersMap.get(path);

        // If there is an argument
        Optional<Integer> listLimit = getLimitArgIfPresent(node);
        if (listLimit.isPresent() && StringUtils.isBlank(multiplier)) {
            double collectionSize = listLimit.get();
            return collectionSize * defaultMultiplier;
        }

        // If it has children with no argument
        if (!isLeaf(node)) {
            return calculateMultiplier(multiplier, context);
        }

        // If its a leaf
        return 1;
    }

    private Double calculateMultiplier(String formula, Map<String, Double> context) {
        if(StringUtils.isBlank(formula)) {
            return defaultMultiplier;
        }

        Matcher matcher = FORMULA_VARIABLE_PATTERN.matcher(formula);
        Map<String,Double> variablesMapping = new HashMap<>();
        for (int i = 0; matcher.find(); i++) {
            String var = matcher.group();
            try {
                String nextVarAlias = ALPHABET[i];
                Double varValue = evaluateContextVariableValue(var, context);
                variablesMapping.put(nextVarAlias, varValue);

                formula = formula.replace(var, nextVarAlias);
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.error("The amount of variables has been exceeded.");
            } catch (Exception e) {
                logger.error("Cannot evaluate ({}) variable of formula.", var);
            }
        }

        double evaluate = 0;
        try {
            Expression expr = new ExpressionBuilder(formula)
                    .variables(variablesMapping.keySet())
                    .build()
                    .setVariables(variablesMapping);

            evaluate = expr.evaluate();
        } catch (Exception e) {
            logger.error("cannot evaluate query complexity formula ({}).", formula);
        }

        return evaluate;
    }

    private Double evaluateContextVariableValue(String var, Map<String, Double> context) {
        if (StringUtils.isBlank(var)) {
            return 0d;
        }

        var = StringUtils.substringBetween(var, FORMULA_VARIABLE_OPEN_TAG, FORMULA_VARIABLE_CLOSS_TAG);
        Double val = context.get(var);
        if (val == null) {
            logger.error("Doesn't support complexity formula variable ({})", var);
            return 0d;
        }
        return val;
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

    public Double getDefaultMultiplier() {
        return defaultMultiplier;
    }
}

