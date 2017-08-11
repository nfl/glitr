package com.nfl.glitr.util;

import com.google.common.reflect.ClassPath;
import com.nfl.glitr.annotation.GlitrForwardPagingArguments;
import com.nfl.glitr.exception.complexity.GlitrComplexityException;
import com.nfl.glitr.exception.complexity.GlitrComplexityMultipleException;
import com.nfl.glitr.exception.complexity.GlitrComplexityValidationException;
import graphql.language.*;
import graphql.parser.Parser;
import graphql.relay.Connection;
import graphql.relay.PageInfo;
import graphql.validation.ValidationErrorType;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class QueryComplexityCalculator {

    private static final Logger logger = LoggerFactory.getLogger(QueryComplexityCalculator.class);

    private int defaultListLimit;
    private Class rootClass;
    private Class mutationDefinitionClass;
    private List<String> domainPackageNames;
    private Map<String, Class> simpleNameToClassMap;
    private Map<String, Class> mutationNameToPayloadClassMap = new HashMap<>();
    private Map<Class, List<java.lang.reflect.Method>> classToAllMethodsMap = new HashMap<>();

    public QueryComplexityCalculator(Class rootClass, Class mutationDefinitionClass, List<String> domainPackageNames) {
        this(rootClass, mutationDefinitionClass, domainPackageNames, 10);
    }

    public QueryComplexityCalculator(Class rootClass, Class mutationDefinitionClass, List<String> domainPackageNames, int defaultListLimit) {
        this.defaultListLimit = defaultListLimit;
        this.domainPackageNames = domainPackageNames;
        this.rootClass = rootClass;
        this.mutationDefinitionClass = mutationDefinitionClass;

        this.initSimpleNameToClassMap();
        this.initMutationNameToPayloadClassMap();
    }

    private void initMutationNameToPayloadClassMap() {
        if (mutationDefinitionClass != null) {
            mutationNameToPayloadClassMap = Arrays.stream(mutationDefinitionClass.getDeclaredMethods())
                    .collect(Collectors.toMap(
                            method -> ReflectionUtil.sanitizeMethodName(method.getName()),
                            Method::getReturnType));
        }
    }

    private void initSimpleNameToClassMap() {
        try {
            ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
            simpleNameToClassMap = domainPackageNames.stream()
                    .flatMap(name -> classPath.getTopLevelClassesRecursive(name).stream())
                    .collect(Collectors.toMap(ClassPath.ClassInfo::getSimpleName, classInfo -> {
                        try {
                            return Class.forName(classInfo.getName());
                        } catch (ClassNotFoundException e) {
                            return null;
                        }
                    }));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public int computeQueryComplexity(String query) {
        Parser parser = new Parser();
        Document document;
        try {
            document = parser.parseDocument(query);
        } catch (ParseCancellationException e) {
            return -1;
        }

        Map<Class, List<Node>> nodeTypeToNodesMap = document.getChildren().stream().collect(Collectors.groupingBy(Object::getClass));
        Map<String, Integer> fragmentNameToScoreMap = buildFragmentToScoreMap(nodeTypeToNodesMap.getOrDefault(FragmentDefinition.class, Collections.EMPTY_LIST));

        int maxScore = 0;
        List<GlitrComplexityException> errors = new ArrayList<>();

        for (Node n : nodeTypeToNodesMap.get(OperationDefinition.class)) {
            try {
                int score = recursiveScore(n, null, fragmentNameToScoreMap, isMutationNode(n), null);
                maxScore = Math.max(maxScore, score);
            } catch (NoSuchMethodException e) {
                errors.add(new GlitrComplexityValidationException(e.getMessage(), ValidationErrorType.FieldUndefined));
            }
        }

        if (!errors.isEmpty()) {
            throw new GlitrComplexityMultipleException(errors);
        }
        return maxScore;
    }

    private Map<String, Integer> buildFragmentToScoreMap(List<FragmentDefinition> fragments) {
        Map<FragmentDefinition, Integer> fragmentToLevelMap = fragments.stream()
                .collect(Collectors.toMap(fragment -> fragment,
                        this::computeNumberOfLevelsOfNestedFragments));

        List<FragmentDefinition> fragmentsSortedByNumberOfLevelsAsc = fragmentToLevelMap.entrySet().stream()
                .sorted((o1, o2) -> o1.getValue() - o2.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());


        Map<String, Integer> calculatedScores = new HashMap<>();
        List<GlitrComplexityException> errors = new ArrayList<>();

        fragmentsSortedByNumberOfLevelsAsc.forEach(fragment -> {
            try {
                int score = calculateScoreOfFragment(fragment, calculatedScores);
                calculatedScores.put(fragment.getName(), score);
            } catch (GlitrComplexityException e) {
                errors.add(e);
            }
        });

        if (!errors.isEmpty()) {
            throw new GlitrComplexityMultipleException(errors);
        }
        return calculatedScores;
    }

    private int computeNumberOfLevelsOfNestedFragments(Node node) {
        int depth = node.getClass() == FragmentSpread.class ? 1 : 0;
        int maxLevels = node.getChildren().stream()
                .map(this::computeNumberOfLevelsOfNestedFragments)
                .reduce(0, Math::max);
        return depth + maxLevels;
    }

    private int calculateScoreOfFragment(FragmentDefinition fragment, Map<String, Integer> fragmentToScoreMap) {
        int score = 0;
        String typeName = fragment.getTypeCondition().getName();
        for (Node n : fragment.getChildren()) {
            try {
                score += recursiveScore(n, simpleNameToClassMap.get(typeName), fragmentToScoreMap);
            } catch (NoSuchMethodException e) {
                throw new GlitrComplexityValidationException(String.format("There is an error in fragment [%s]. %s", fragment.getName(), e.getMessage()), ValidationErrorType.FieldUndefined);
            }
        }
        return score;
    }

    private int recursiveScore(Node node, Class parentClass, Map<String, Integer> fragmentNameToScoreMap) throws NoSuchMethodException {
        return recursiveScore(node, parentClass, fragmentNameToScoreMap, false, null);
    }

    private int recursiveScore(Node node, Class parentClass, Map<String, Integer> fragmentNameToScoreMap, boolean isMutation, Class typeOfConnection) throws NoSuchMethodException {
        int score = 0;
        int multiplier = 1;
        Class nodeClass = parentClass;
        Class newTypeOfConnection = typeOfConnection;

        if (node.getClass() == Field.class) {
            score = 1;
            Field field = (Field) node;

            java.lang.reflect.Type type = getMethodFromField(field, parentClass);
            if (type != null && ParameterizedType.class.isAssignableFrom(type.getClass())) {
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
                multiplier = extractMultiplierFromListField(field);
            }
            nodeClass = (Class) type;

            if (isConnection(field, nodeClass, parentClass)) {
                newTypeOfConnection = nodeClass;
                nodeClass = Connection.class;
                multiplier = 1;
            } else if (isNodeOfConnection(nodeClass, parentClass)) {
                nodeClass = typeOfConnection;
            } else if (nodeClass == null && isMutation) {
                nodeClass = mutationNameToPayloadClassMap.get(field.getName());
            }
        } else if (node.getClass() == InlineFragment.class) {
            String typeName = ((InlineFragment) node).getTypeCondition().getName();
            nodeClass = simpleNameToClassMap.get(typeName);
        } else if (node.getClass() == FragmentSpread.class) {
            String fragmentName = ((FragmentSpread) node).getName();
            score += fragmentNameToScoreMap.getOrDefault(fragmentName, 0);
        }

        for (Node n : node.getChildren()) {
            score += multiplier * recursiveScore(n, nodeClass, fragmentNameToScoreMap, isMutation, newTypeOfConnection);
        }
        return score;
    }

    private java.lang.reflect.Type getMethodFromField(Field field, Class parentClass) throws NoSuchMethodException {

        if (!shouldCheckClassForField(parentClass, field)) {
            return parentClass;
        }

        Class actualParentClass = parentClass;
        String methodName = getMethodNameFromField(field);
        if (actualParentClass == null) {
            try {
                rootClass.getDeclaredMethod(methodName);
                actualParentClass = rootClass;
            } catch (NoSuchMethodException e) {
                logger.info("No method named {} in class {}", methodName, rootClass.getName());
            }
        }

        if (actualParentClass != null) {
            return getInheritedMethodFromClass(methodName, actualParentClass).getGenericReturnType();
        }

        return null;
    }

    private int extractMultiplierFromListField(Field node) {
        Optional<Argument> listLimit = node.getArguments().stream()
                .filter(argument -> argument.getName().equals("first"))
                .findFirst();
        if (listLimit.isPresent()) {
            return ((IntValue) listLimit.get().getValue()).getValue().intValue();
        }
        return defaultListLimit;
    }

    private Method getInheritedMethodFromClass(String methodName, Class clazz) throws NoSuchMethodException {
        List<Method> allMethods = classToAllMethodsMap.computeIfAbsent(clazz, key -> {
            List<Method> methods = asList(key.getMethods());
            classToAllMethodsMap.put(key, methods);
            return methods;
        });
        return allMethods.stream()
                .filter(f -> f.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(String.format("There is no method named %s in class %s", methodName, clazz.getName())));
    }

    private boolean shouldCheckClassForField(Class clazz, Field field) {
        //Don't want to check if class is PageInfo b/c when executing the query, PageInfo could be PageInfoWithTotal
        //but no way to know just by looking at query, so the score would fail if total was in query even though
        //PageInfo will be PageInfoWithTotal when executed
        return (clazz != Object.class || !field.getName().equals("id")) && clazz != PageInfo.class;
    }

    private boolean isConnection(Field field, Class nodeClass, Class parentClass) throws NoSuchMethodException {
        return nodeClass != null && parentClass == null && isConnection(field);
    }

    private boolean isConnection(Field field) throws NoSuchMethodException {
        String method = getMethodNameFromField(field);
        return Arrays.stream(rootClass.getMethod(method).getDeclaredAnnotations())
                .anyMatch(annotation -> annotation.annotationType() == GlitrForwardPagingArguments.class);
    }

    private boolean isNodeOfConnection(Class nodeClass, Class parentClass) {
        return nodeClass == Object.class && parentClass != null;
    }

    private boolean isMutationNode(Node node) {
        return node != null
                && node.getClass() == OperationDefinition.class
                && ((OperationDefinition) node).getOperation().equals(OperationDefinition.Operation.MUTATION);
    }

    private String getMethodNameFromField(Field field) {
        return "get" + StringUtils.capitalize(field.getName());
    }
}

