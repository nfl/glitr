package com.nfl.glitr.calculator;

import com.nfl.glitr.exception.GlitrException;
import com.nfl.glitr.registry.schema.GlitrFieldDefinition;
import com.nfl.glitr.registry.schema.GlitrMetaDefinition;
import com.nfl.glitr.registry.schema.GraphQLConnectionList;
import graphql.language.*;
import graphql.parser.Parser;
import graphql.schema.*;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nfl.glitr.util.NodeUtil.COMPLEXITY_FORMULA_KEY;
import static com.nfl.glitr.util.NodeUtil.COMPLEXITY_IGNORE_KEY;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
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
    private GraphQLSchema schema;

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

    public QueryComplexityCalculator withSchema(GraphQLSchema schema) {
        this.schema = schema;
        return this;
    }

    /**
     * @param query - graphql query string
     * @param variables graphQL query variables
     * We want to validate the query or fail fast.
     */
    public void validate(String query, Map<String, Object> variables) {
        if (characterLimitExceeded(query)) {
            throw new GlitrException(String.format("query length has exceeded the maximum of %d characters.", maxCharacterLimit));
        }

        if (depthLimitExceeded(query)) {
            throw new GlitrException(String.format("query depth has exceeded the maximum depth level of %d.", maxDepthLimit));
        }

        if (scoreLimitExceeded(query, variables)){
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

        QueryComplexityNode entryPoint = buildComplexitySchema(query);
        GraphQLFieldDefinition rootField = getRootField(query, entryPoint.getName());
        boolean connection = rootField != null && rootField.getType() instanceof GraphQLConnectionList;

        return calculateMaxDepth(entryPoint, rootField, connection);
    }

    /**
     * @param parentNode Root node to recursively iterate and find the deepest child.
     * @param rootField Root GraphQLObject which contains all node's meta info.
     * @param connectionNode Specify whether parentNode is connection node or not.
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
    private int calculateMaxDepth(QueryComplexityNode parentNode, GraphQLFieldDefinition rootField, boolean connectionNode) {
        int depth = 0;
        boolean ignoreNode = parentNode.isIgnore() || (rootField != null && isTrue(getGraphQLMeta(rootField, COMPLEXITY_IGNORE_KEY)));
        if (!ignoreNode && !isLeaf(parentNode) && !connectionNode) {
            depth = 1;
        }

        int maxChildDepth = 0;
        for (QueryComplexityNode childNode : parentNode.getChildren()) {
            boolean childConnectionNode = isConnectionNode(rootField, childNode.getName());
            GraphQLFieldDefinition childObject = getGraphQLObject(rootField, childNode.getName());

            final int currentChildDepth = calculateMaxDepth(childNode, childObject, childConnectionNode);
            maxChildDepth = Math.max(maxChildDepth, currentChildDepth);
        }

        depth += maxChildDepth;
        return depth;
    }

    /**
     * @param query graphql query string
     * @param variables graphql query variables
     * @return true if the {@link #queryScore(String, Map) query's score} result is greater than the maximum allowed score.
     */
    public boolean scoreLimitExceeded(String query, Map<String, Object> variables) {
        return queryScore(query, variables) > maxScoreLimit;
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
    * @param variables graphQL query variables
    * @return query score as an double.  The way the query score is calculated is by summing the multipliers of all nodes.
    **/
    public double queryScore(String query, Map<String, Object> variables) {
        return queryScoreDetails(query, variables).getTotalWeight();
    }

    /**
     *
     * @param query  string
     * @param variables graphQL query variables
     * @return {@link QueryComplexityNode}
     */
    public QueryComplexityNode queryScoreDetails(String query, Map<String, Object> variables) {
        if (StringUtils.isBlank(query)) {
            throw new GlitrException("query cannot be null or empty");
        }

        QueryComplexityNode entryPoint = buildComplexitySchema(query);
        GraphQLFieldDefinition rootField = getRootField(query, entryPoint.getName());
        boolean connection = rootField != null && rootField.getType() instanceof GraphQLConnectionList;
        return queryScoreDetails(entryPoint, rootField, 0, new HashMap<>(), connection, variables);
    }

    private QueryComplexityNode buildComplexitySchema(String query) {
        OperationDefinition rootNode = parseRootNode(query);
        Field entryPoint = getFirstFieldNode(rootNode);
        if (entryPoint == null) {
            return new QueryComplexityNode();
        }

        return buildComplexitySchema(entryPoint, new QueryComplexityNode(entryPoint.getName(), rootNode.getOperation() == OperationDefinition.Operation.MUTATION));
    }

    private Field getFirstFieldNode(OperationDefinition rootNode) {
        return (Field) Optional.ofNullable(rootNode)
                .map(OperationDefinition::getChildren)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(nodes -> nodes.stream().filter(node -> node instanceof SelectionSet).findFirst())
                .map(Node::getChildren)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(nodes -> nodes.stream().filter(node -> node instanceof Field).findFirst())
                .orElse(null);
    }

    private QueryComplexityNode buildComplexitySchema(Node queryNode, QueryComplexityNode queryComplexityParentNode) {
        for (Node child : (List<Node>) queryNode.getChildren()) {
            QueryComplexityNode childComplexity = queryComplexityParentNode;
            if (child instanceof Argument) {
                queryComplexityParentNode.addArgument((Argument) child);
            } else if (isAuthorizedField(child)) {
                childComplexity = new QueryComplexityNode(((Field) child).getName());
                queryComplexityParentNode.addChild(childComplexity);
            }
            buildComplexitySchema(child, childComplexity);
        }

        return queryComplexityParentNode;
    }

    private String getMutationName(String query) {
        return trimToNull(substringBetween(query, "{", "("));
    }

    private GraphQLFieldDefinition getRootField(String query, String entryPoint) {
        if (schema == null) {
            return null;
        }

        boolean mutationQuery = isMutation(query);
        GraphQLObjectType rootType = mutationQuery ? schema.getMutationType() : schema.getQueryType();
        return rootType.getFieldDefinition(entryPoint);
    }

    @SuppressWarnings("unchecked")
    private QueryComplexityNode queryScoreDetails(QueryComplexityNode parentNode, GraphQLFieldDefinition graphQlObject, int depth, Map<String, Double> nestedContext, boolean connectionNode, Map<String, Object> queryVariables) {
        Map<String, Double> context = refreshQueryContext(nestedContext, parentNode, queryVariables);

        Boolean ignoreComplexity = getGraphQLMeta(graphQlObject, COMPLEXITY_IGNORE_KEY);
        depth = !connectionNode && !parentNode.isIgnore() && isNotTrue(ignoreComplexity) ? (depth + 1) : depth;

        double childScores = 0d;
        for (QueryComplexityNode currentChild : parentNode.getChildren()) {
            boolean childConnectionNode = isConnectionNode(graphQlObject, currentChild.getName());
            GraphQLFieldDefinition childObject = getGraphQLObject(graphQlObject, currentChild.getName());
            QueryComplexityNode childComplexityNode = queryScoreDetails(currentChild, childObject, depth , context, childConnectionNode, queryVariables);
            childScores += childComplexityNode.getTotalWeight();
        }

        double currentNodeScore = 0d;
        if (!connectionNode && !parentNode.isIgnore() && isNotTrue(ignoreComplexity)) {
            Map<String, Double> nodeContext = buildContext(parentNode, nestedContext, depth, childScores, queryVariables);
            currentNodeScore = extractMultiplierFromListField(parentNode, nodeContext, graphQlObject, queryVariables);
            parentNode.setWeight(currentNodeScore);
        }
        parentNode.setTotalWeight(currentNodeScore + childScores);

        return parentNode;
    }

    private GraphQLFieldDefinition getGraphQLObject(GraphQLFieldDefinition graphQlObject, String name) {
        if (graphQlObject == null) {
            return null;
        }

        try {
            GraphQLOutputType objType = graphQlObject.getType();
            if (objType instanceof GraphQLModifiedType) {
                return ((GraphQLFieldsContainer) ((GraphQLModifiedType) objType).getWrappedType()).getFieldDefinition(name);
            } else if (objType instanceof GraphQLFieldsContainer) {
                return ((GraphQLFieldsContainer) objType).getFieldDefinition(name);
            }
        } catch (Exception e) {
            logger.error("Cannot process graphQL object");
        }

        return null;
    }

    private <T> T getGraphQLMeta(GraphQLFieldDefinition graphQlObject, String name) {
        Optional<Set<GlitrMetaDefinition>> metaDefinitions = Optional.ofNullable(graphQlObject)
                .map(GraphQLFieldDefinition::getDefinition)
                .filter(x -> x instanceof GlitrFieldDefinition)
                .map(x -> ((GlitrFieldDefinition)x).getMetaDefinitions())
                .filter(CollectionUtils::isNotEmpty);

        if (metaDefinitions.isPresent()) {
            return metaDefinitions.get().stream()
                    .filter(metaDefinition -> name.equals(metaDefinition.getName()))
                    .findFirst()
                    .map(metaDefinition -> (T) metaDefinition.getValue())
                    .orElse(null);
        }

        return null;
    }

    private boolean isConnectionNode(GraphQLFieldDefinition graphQlObject, String name) {
        if (graphQlObject == null) {
            return false;
        }

        GraphQLType objType = graphQlObject.getType();
        if (objType instanceof GraphQLConnectionList) {
            return true;
        }

        if (objType instanceof GraphQLObjectType) {
            GraphQLFieldDefinition fieldDefinition = ((GraphQLObjectType) objType).getFieldDefinition(name);
            return fieldDefinition != null && fieldDefinition.getType() instanceof GraphQLConnectionList;
        }

        return false;
    }

    private Map<String, Double> refreshQueryContext(Map<String, Double> queryContext, QueryComplexityNode field, Map<String, Object> queryVariables) {
        int limit = getLimitArgIfPresent(field, queryVariables).orElse(0);

        Double parentCollectionsSize = queryContext.getOrDefault("totalCollectionsSize", 0d);
        Double totalCollectionsSize = parentCollectionsSize + limit;
        queryContext.put("totalCollectionsSize", totalCollectionsSize);

        return new HashMap<>(queryContext);
    }

    private Map buildContext(QueryComplexityNode field, Map<String, Double> queryContext, int depth, double childScores, Map<String, Object> queryVariables) {
        double totalCollectionsSize = queryContext.getOrDefault("totalCollectionsSize", 0d);
        int limit = getLimitArgIfPresent(field, queryVariables).orElse(0);

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

    private boolean isAuthorizedField(Node node) {
        return node != null && node instanceof Field;
    }

    private Optional<Integer> getLimitArgIfPresent(QueryComplexityNode node, Map<String, Object> queryVariables) {
        if (CollectionUtils.isEmpty(node.getArguments())) {
            return Optional.empty();
        }

        Optional<Argument> listLimit = node.getArguments().stream()
                .filter(argument -> argument.getName().equals("first"))
                .findFirst();

        Integer limit = null;
        if (listLimit.isPresent()) {
            Value value = listLimit.get().getValue();
            if (value instanceof VariableReference && queryVariables != null) {
                limit = (Integer) queryVariables.get(((VariableReference) value).getName());
            } else if (value instanceof IntValue) {
                limit = ((IntValue) value).getValue().intValue();
            }
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
     * If the node is a leaf node, we return 0. Exception is if the leaf node is marked with {@link com.nfl.glitr.annotation.GlitrQueryComplexity @GlitrQueryComplexity}.
     * {
     *     playerName
     * }
     * *****************************************************************************************************************
     * *****************************************************************************************************************
     * </pre>
     *
     * @param node The field node we would like to test against
     * @param context mutable current branch context
     * @param graphQlObject {@link GraphQLFieldDefinition}
     * @param queryVariables graphQL query variables
     * @return the multiplier
     *
     **/
    protected double extractMultiplierFromListField(QueryComplexityNode node, Map<String, Double> context, GraphQLFieldDefinition graphQlObject, Map<String, Object> queryVariables) {
        String multiplier = getGraphQLMeta(graphQlObject, COMPLEXITY_FORMULA_KEY);

        // If there is an argument
        Optional<Integer> listLimit = getLimitArgIfPresent(node, queryVariables);
        if (listLimit.isPresent() && StringUtils.isBlank(multiplier)) {
            double collectionSize = listLimit.get();
            return collectionSize * defaultMultiplier;
        }

        // If it has children with no argument
        if (!isLeaf(node) || StringUtils.isNotBlank(multiplier)) {
            return calculateMultiplier(multiplier, context);
        }

        // If its a leaf
        return 0;
    }

    private Double calculateMultiplier(String formula, Map<String, Double> context) {
        if (StringUtils.isBlank(formula)) {
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
                logger.error("The amount of variables in formula ({}) has been exceeded.", formula);
            } catch (Exception e) {
                logger.error("Cannot evaluate ({}) variable of formula ({}).", var, formula);
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

    /**
     * @param node - Node to test against
     * @return  true  --> if the node doesn't have children
     *          false --> if the node has children.
     */
    private boolean isLeaf(QueryComplexityNode node) {
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

