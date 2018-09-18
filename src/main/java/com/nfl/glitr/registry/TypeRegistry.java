package com.nfl.glitr.registry;

import com.google.common.collect.ImmutableMap;
import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.annotation.GlitrArgument;
import com.nfl.glitr.annotation.GlitrDescription;
import com.nfl.glitr.annotation.GlitrQueryComplexity;
import com.nfl.glitr.exception.GlitrException;
import com.nfl.glitr.registry.datafetcher.AnnotationBasedDataFetcherFactory;
import com.nfl.glitr.registry.datafetcher.query.OverrideDataFetcher;
import com.nfl.glitr.registry.datafetcher.query.batched.CompositeDataFetcherFactory;
import com.nfl.glitr.registry.schema.GlitrFieldDefinition;
import com.nfl.glitr.registry.schema.GlitrMetaDefinition;
import com.nfl.glitr.registry.type.*;
import com.nfl.glitr.relay.Node;
import com.nfl.glitr.relay.Relay;
import com.nfl.glitr.util.ReflectionUtil;
import graphql.TypeResolutionEnvironment;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func4;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.nfl.glitr.util.NodeUtil.COMPLEXITY_FORMULA_KEY;
import static com.nfl.glitr.util.NodeUtil.COMPLEXITY_IGNORE_KEY;
import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * The heart of GLiTR, the class is responsible for the creation of the GraphQL schema by using reflection to
 * recursively inspect the passed domain
 */
public class TypeRegistry implements TypeResolver {

    private static final Logger logger = LoggerFactory.getLogger(TypeRegistry.class);

    private static final String ARG_NULLABILITY = "nullability";
    private static final String ARG_VIEW_ARGUMENT = "viewArgument";
    private static final GraphQLScalarType GraphQLNonBlankString = new GraphQLScalarType("NonBlankString", "Built-in Non Blank String", new Coercing<String, String>() {
        @Override
        public String serialize(Object input) {
            if (StringUtils.isBlank((String) input)) {
                throw new CoercingSerializeException(
                        "Expected non blank 'StrValue'."
                );
            }
            return input.toString();
        }

        @Override
        public String parseValue(Object input) {
            if (StringUtils.isBlank((CharSequence) input)) {
                throw new CoercingParseValueException(
                        "Expected non blank 'StrValue'."
                );
            }
            return serialize(input);
        }

        @Override
        public String parseLiteral(Object input) {
            if (!(input instanceof StringValue)) {
                throw new CoercingParseLiteralException(
                        "Expected AST type 'StrValue' but was '" + typeName(input) + "'."
                );
            }
            if (StringUtils.isBlank(((StringValue) input).getValue())) {
                throw new CoercingParseLiteralException(
                        "Expected non blank 'StrValue'."
                );
            }
            return ((StringValue) input).getValue();
        }
    });
    public static final GraphQLScalarType GraphQLNonBlankID = new GraphQLScalarType("NonBlankID", "Built-in ID", new Coercing<Object, Object>() {

        private String convertImpl(Object input) {
            if (input instanceof String) {
                return (String) input;
            }
            if (input instanceof Integer) {
                return String.valueOf(input);
            }
            if (input instanceof Long) {
                return String.valueOf(input);
            }
            if (input instanceof UUID) {
                return String.valueOf(input);
            }
            return null;

        }

        @Override
        public String serialize(Object input) {
            String result = convertImpl(input);
            if (result == null) {
                throw new CoercingSerializeException(
                        "Expected type 'ID' but was '" + typeName(input) + "'."
                );
            }
            if (StringUtils.isBlank(result)) {
                throw new CoercingSerializeException(
                        "Expected non blank 'ID'."
                );
            }
            return result;
        }

        @Override
        public String parseValue(Object input) {
            String result = convertImpl(input);
            if (result == null) {
                throw new CoercingParseValueException(
                        "Expected type 'ID' but was '" + typeName(input) + "'."
                );
            }
            if (StringUtils.isBlank(result)) {
                throw new CoercingParseValueException(
                        "Expected non blank 'ID'."
                );
            }
            return result;
        }

        @Override
        public String parseLiteral(Object input) {
            if (input instanceof StringValue) {
                if (StringUtils.isBlank(((StringValue) input).getValue())) {
                    throw new CoercingParseLiteralException(
                            "Expected non blank 'ID'."
                    );
                }
                return ((StringValue) input).getValue();
            }
            if (input instanceof IntValue) {
                return ((IntValue) input).getValue().toString();
            }
            throw new CoercingParseLiteralException(
                    "Expected AST type 'IntValue' or 'StringValue' but was '" + typeName(input) + "'."
            );
        }
    });

    private final Map<Class, GraphQLType> registry = new ConcurrentHashMap<>();
    private final Map<String, GraphQLType> nameRegistry = new ConcurrentHashMap<>();
    private final Map<Class, List<Object>> overrides;

    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap;
    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap;
    private final Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap;
    private final Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap;
    private final Map<Class, GraphQLType> javaTypeDeclaredAsScalarMap;

    private GraphQLInterfaceType nodeInterface;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private Relay relay;
    private boolean explicitRelayNodeScanEnabled;

    private GraphQLTypeFactory graphQLTypeFactory = new GraphQLTypeFactory()
            .withOutputTypeFactory(new GraphQLEnumTypeFactory(), JavaType.ENUM)
            .withOutputTypeFactory(new GraphQLInterfaceTypeFactory(this), JavaType.INTERFACE)
            .withOutputTypeFactory(new GraphQLInterfaceTypeFactory(this), JavaType.ABSTRACT_CLASS)
            .withOutputTypeFactory(new GraphQLObjectTypeFactory(this), JavaType.CLASS)
            .withInputTypeFactory(new GraphQLInputObjectTypeFactory(this), JavaType.ABSTRACT_CLASS, JavaType.CLASS, JavaType.INTERFACE);


    TypeRegistry(Map<Class, List<Object>> overrides, Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap, Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap, Map<Class, GraphQLType> javaTypeDeclaredAsScalarMap, Relay relay, boolean explicitRelayNodeScanEnabled) {
        this.overrides = overrides;
        this.annotationToDataFetcherFactoryMap = annotationToDataFetcherFactoryMap;
        this.annotationToDataFetcherMap = annotationToDataFetcherMap;
        this.annotationToArgumentsProviderMap = annotationToArgumentsProviderMap;
        this.annotationToGraphQLOutputTypeMap = annotationToGraphQLOutputTypeMap;
        this.javaTypeDeclaredAsScalarMap = javaTypeDeclaredAsScalarMap;
        this.relay = relay;
        if (relay != null) {
            this.nodeInterface = relay.nodeInterface(this);
            // register Node so we don't inadvertently recreate it later
            this.registry.put(Node.class, this.nodeInterface);
            this.nameRegistry.put(Node.class.getSimpleName(), this.nodeInterface);
        }
        this.explicitRelayNodeScanEnabled = explicitRelayNodeScanEnabled;
    }

    /**
     * Type Dictionary provides a way to pass a set of types into the schema when building it, which will be added into
     * the set of available types when replacing type references
     *
     * @return set of {@link GraphQLType} to be passed to {@link GraphQLSchema}
     */
    public Set<GraphQLType> getTypeDictionary() {
        return new HashSet<>(registry.values());
    }

    @Nullable
    public GraphQLInterfaceType getNodeInterface() {
        return nodeInterface;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        return (GraphQLObjectType)registry.get(env.getObject().getClass());
    }

    public Map<Class, GraphQLType> getRegistry() {
        return registry;
    }

    public boolean isExplicitRelayNodeScanEnabled() {
        return explicitRelayNodeScanEnabled;
    }

    /**
     * Root class should be passed here so the graph can be inspected in its entirety
     *
     * @param clazz top level Class from which to begin introspection
     * @return GraphQLType
     */
    public GraphQLType lookup(Class clazz) {
        // do a first pass lookup
        lookupOutput(clazz);
        // go over all the types in the type map and do another pass to resolve the GraphQL type references
        // remove the type reference to be replaced by the resolved GraphQLType and lookup again
        registry.entrySet().stream()
                .filter(graphQLTypeEntry -> graphQLTypeEntry.getValue() instanceof GraphQLTypeReference)
                .forEach(graphQLTypeEntry -> {
                    registry.remove(graphQLTypeEntry.getKey());
                    lookupOutput(graphQLTypeEntry.getKey());
                });

        return registry.get(clazz);
    }

    /**
     * Check if the given class if found the registry, if not, first create it ({@link GraphQLOutputType}), next add it
     * to the registry
     *
     * @param clazz class on which to preform introspection
     * @return {@link GraphQLType}
     */
    public GraphQLType lookupOutput(Class clazz) {
        if (registry.containsKey(clazz)) {
            return registry.get(clazz);
        } else if (nameRegistry.containsKey(clazz.getSimpleName())) {
            return nameRegistry.get(clazz.getSimpleName());
        }

        // put a type reference in while building the type to work around circular references
        registry.put(clazz, new GraphQLTypeReference(clazz.getSimpleName()));
        nameRegistry.put(clazz.getSimpleName(), new GraphQLTypeReference(clazz.getSimpleName()));

        GraphQLOutputType type = graphQLTypeFactory.createGraphQLOutputType(clazz);

        if (type != null) {
            registry.put(clazz, type);
            nameRegistry.put(clazz.getSimpleName(), type);
        } else {
            throw new IllegalArgumentException("Unable to create GraphQLOutputType for: " + clazz.getCanonicalName());
        }

        return type;
    }

    /**
     * Check if the given class is found in the registry, if not, first create it ({@link GraphQLInputType}), then add it
     * to the registry
     *
     * @param clazz class on which to preform input introspection
     * @return {@link GraphQLType}
     */
    public GraphQLType lookupInput(Class clazz) {
        if (registry.containsKey(clazz) && !(registry.get(clazz) instanceof GraphQLTypeReference)) {
            return registry.get(clazz);
        } else if (nameRegistry.containsKey(clazz.getSimpleName()) && !(nameRegistry.get(clazz.getSimpleName()) instanceof GraphQLTypeReference)) {
            return nameRegistry.get(clazz.getSimpleName());
        }

        GraphQLInputType type = graphQLTypeFactory.createGraphQLInputType(clazz);

        if (type != null) {
            registry.put(clazz, type);
            nameRegistry.put(clazz.getSimpleName(), type);
        } else {
            throw new IllegalArgumentException("Unable to create GraphQLInputType for: " + clazz.getCanonicalName());
        }

        return type;
    }

    public GraphQLObjectType createRelayMutationType(Class clazz) {
        Map<String, Pair<Method, Class>> methods = ReflectionUtil.getMethodMap(clazz);
        addExtraMethodsToTheSchema(clazz, methods);

        List<GraphQLFieldDefinition> fields = methods.values().stream()
                .map(pair -> getGraphQLFieldDefinition(clazz, pair))
                .collect(Collectors.toList());

        if (fields.size() == 0) {
            // GraphiQL doesn't like objects with no fields, so add an unused field to be safe
            fields.add(newFieldDefinition().name("unused_fields_dead_object").type(GraphQLBoolean).staticValue(false).build());
        }

        GraphQLObjectType.Builder builder = newObject()
                .name(clazz.getSimpleName())
                .description(ReflectionUtil.getDescriptionFromAnnotatedElement(clazz))
                .fields(fields);

        return builder.build();
    }

    private GraphQLFieldDefinition getGraphQLFieldDefinition(Class clazz, Pair<Method, Class> pair) {
        // GraphQL Field Name
        Method method = pair.getLeft();
        String name = ReflectionUtil.sanitizeMethodName(method.getName());

        // DataFetcher
        Class declaringClass = pair.getRight();
        List<DataFetcher> fetchers = retrieveDataFetchers(clazz, declaringClass, method);
        DataFetcher dataFetcher = createDataFetchersFromDataFetcherList(fetchers, declaringClass, name);

        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);
        if (description != null && description.equals(GlitrDescription.DEFAULT_DESCRIPTION)) {
            description = ReflectionUtil.getDescriptionFromAnnotatedField(clazz, method);
        }

        Optional<GlitrQueryComplexity> glitrQueryComplexity = ReflectionUtil.getAnnotationOfMethodOrField(clazz, method, GlitrQueryComplexity.class);
        Set<GlitrMetaDefinition> metaDefinitions = new HashSet<>();
        glitrQueryComplexity.ifPresent(queryComplexity -> {
            metaDefinitions.add(new GlitrMetaDefinition(COMPLEXITY_FORMULA_KEY, queryComplexity.value()));
            metaDefinitions.add(new GlitrMetaDefinition(COMPLEXITY_IGNORE_KEY, queryComplexity.ignore()));
        });

        return newFieldDefinition()
                .name(name)
                .description(description)
                .type(retrieveGraphQLOutputType(declaringClass, method))
                .argument(createRelayInputArgument(declaringClass, method))
                .dataFetcher(dataFetcher)
                .definition(new GlitrFieldDefinition(name, metaDefinitions))
                // TODO: static value
                // TODO: deprecation reason
                .build();
    }

    public static DataFetcher createDataFetchersFromDataFetcherList(List<DataFetcher> fetchers, Class declaringClass, String name) {
        try {
            return CompositeDataFetcherFactory.create(fetchers);
        } catch (IllegalArgumentException e) {
            throw new GlitrException(e.getMessage() + " For " + declaringClass + "." + name);
        }
    }

    private GraphQLArgument createRelayInputArgument(Class methodDeclaringClass, Method method) {
        GlitrArgument[] annotatedGetterGlitrArguments = ReflectionUtil.getArgumentsFromMethod(method);
        if (annotatedGetterGlitrArguments.length != 1) {
            throw new IllegalArgumentException("Only one @GlitrArgument annotation can be placed on a relay mutation for class "+methodDeclaringClass.getSimpleName()+" and method "+ method.getName());
        }

        GlitrArgument arg = annotatedGetterGlitrArguments[0];
        if (!arg.name().equals("input")) {
            throw new IllegalArgumentException("@GlitrArgument annotation name must be `input` for class "+methodDeclaringClass.getSimpleName()+" and method "+ method.getName());
        }

        GraphQLInputType inputType = (GraphQLInputType) convertToGraphQLInputType(arg.type(), arg.name());
        if (arg.nullability() == GlitrArgument.NULLABILITY_NON_NULL) {
            inputType = new GraphQLNonNull(inputType);
        }

        Object defaultValueInput = arg.defaultValue().equalsIgnoreCase(GlitrArgument.NO_DEFAULT_VALUE) ? null : arg.defaultValue();
        if (defaultValueInput != null && arg.type().isEnum()) {
            try {
                defaultValueInput = Enum.valueOf(arg.type(), arg.defaultValue());
            } catch (IllegalArgumentException iae) {
                defaultValueInput = null;
                logger.error("Invalid enum detected as defaultValue [" + arg.defaultValue() + "].  Defaulting to empty defaultValue.", iae);
            }
        }

        return newArgument()
                .name(arg.name())
                .description(arg.description())
                .type(inputType)
                .defaultValue(defaultValueInput)
                .build();
    }

    /**
     * We allow defining GraphQL additional fields outside of the inspected class. In fact, if the getter-method name
     * conflicts with an existing method defined in the inspected class, it won't be added.
     *
     * @param clazz inspected class
     * @param methods original set of eligible methods defined in the inspected class
     */
    public void addExtraMethodsToTheSchema(Class clazz, Map<String, Pair<Method, Class>> methods) {
        if (overrides.containsKey(clazz)) {
            for (Object override : overrides.get(clazz)) {
                for (Method method : override.getClass().getMethods()) {
                    if (!ReflectionUtil.eligibleMethod(method)) {
                        continue;
                    }
                    methods.putIfAbsent(method.getName(), Pair.of(method, override.getClass()));
                }
            }
        }
    }

    public List<DataFetcher> retrieveDataFetchers(Class clazz, Class declaringClass, Method method) {
        List<DataFetcher> fetchers = new ArrayList<>();
        String name = ReflectionUtil.sanitizeMethodName(method.getName());

        // override fetchers
        if (overrides.containsKey(clazz)) {
            fetchers.addAll(overrides.get(clazz).stream()
                    .map(override -> new OverrideDataFetcher(name, override))
                    .collect(Collectors.toList()));
        }

        // we add a default OverrideDataFetcher for override getters in the actual class itself
        fetchers.add(new OverrideDataFetcher(name, clazz));

        DataFetcher annotationDataFetcher = getAnnotationDataFetcherFromMethodOrField(declaringClass, method, name);
        if (annotationDataFetcher != null) {
            fetchers.add(annotationDataFetcher);
        }
        // default fetcher
        fetchers.add(new PropertyDataFetcher(name));

        return fetchers;
    }

    private DataFetcher getAnnotationDataFetcherFromMethodOrField(Class declaringClass, Method method, String fieldName) {
        // inspect annotations on getter
        DataFetcher dataFetcherFromGetter = getDataFetcherFromAnnotationsOnGetter(declaringClass, method);
        if (dataFetcherFromGetter != null) {
            return dataFetcherFromGetter;
        }

        // inspect annotations on field of same name
        Field field = ReflectionUtil.getFieldByName(declaringClass, fieldName);
        if (field == null) {
            return null;
        }

        DataFetcher dataFetcherFromField = getDataFetcherFromAnnotationsOnField(declaringClass, method, field);
        if (dataFetcherFromField != null) {
            return dataFetcherFromField;
        }

        // no custom annotations found
        return null;
    }

    private DataFetcher getDataFetcherFromAnnotationsOnField(Class declaringClass, Method method, Field field) {
        if (field != null) {
            Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
            for (Annotation annotation: fieldAnnotations) {
                // custom fetchers
                Class annotationType = annotation.annotationType();
                if (annotationToDataFetcherFactoryMap.containsKey(annotationType)) {
                    AnnotationBasedDataFetcherFactory annotationBasedDataFetcherFactory = annotationToDataFetcherFactoryMap.get(annotationType);
                    DataFetcher dataFetcher = annotationBasedDataFetcherFactory.create(field, method, declaringClass, annotation);
                    if (dataFetcher != null) {
                        return dataFetcher;
                    }
                } else if (annotationToDataFetcherMap.containsKey(annotationType)) {
                    return annotationToDataFetcherMap.get(annotationType);
                }
            }
        }
        return null;
    }

    private DataFetcher getDataFetcherFromAnnotationsOnGetter(Class declaringClass, Method method) {
        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
        for (Annotation annotation: methodAnnotations) {
            // custom fetchers
            Class annotationType = annotation.annotationType();
            if (annotationToDataFetcherFactoryMap.containsKey(annotationType)) {
                AnnotationBasedDataFetcherFactory annotationBasedDataFetcherFactory = annotationToDataFetcherFactoryMap.get(annotationType);
                DataFetcher dataFetcher = annotationBasedDataFetcherFactory.create(null, method, declaringClass, annotation);
                if (dataFetcher != null) {
                    return dataFetcher;
                }
            } else if (annotationToDataFetcherMap.containsKey(annotationType)) {
                return annotationToDataFetcherMap.get(annotationType);
            }
        }
        return null;
    }

    public List<GraphQLArgument> retrieveArguments(Class declaringClass, Method method) {
        List<GraphQLArgument> arguments = new ArrayList<>();
        String name = ReflectionUtil.sanitizeMethodName(method.getName());

        // inspect annotations on getter
        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
        arguments.addAll(getGraphQLArgumentsFromAnnotations(null, method, declaringClass, methodAnnotations));

        // inspect annotations on field of same name
        Field field = ReflectionUtil.getFieldByName(declaringClass, name);
        if (field != null) {
            Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
            arguments.addAll(getGraphQLArgumentsFromAnnotations(field, method, declaringClass, fieldAnnotations));
        }

        // default argument, argument annotation can be found on either the method or field but method level args take precedence
        GlitrArgument[] annotatedGlitrArguments;
        GlitrArgument[] annotatedGetterGlitrArguments = ReflectionUtil.getArgumentsFromMethod(method);
        GlitrArgument[] annotatedFieldGlitrArguments = ReflectionUtil.getArgumentsFromField(field);

        if (annotatedGetterGlitrArguments.length == 0) {
            annotatedGlitrArguments = annotatedFieldGlitrArguments;
        } else {
            annotatedGlitrArguments = annotatedGetterGlitrArguments;
        }

        if (annotatedGetterGlitrArguments.length > 0 && annotatedFieldGlitrArguments.length > 0) {
            String fieldName = (field != null) ? field.getName() : "null";
            logger.warn("Both Method and Field level @GlitrArgument(s) found, ignoring annotations [{}] for Field [{}]", annotatedFieldGlitrArguments, fieldName);
        }

        arguments.addAll(Arrays.stream(annotatedGlitrArguments)
                .map(this::getGraphQLArgument)
                .collect(Collectors.toList()));

        return arguments;
    }

    private List<GraphQLArgument> getGraphQLArgumentsFromAnnotations(Field field, Method method, Class declaringClass, Annotation[] annotations) {
        List<GraphQLArgument> argumentList = new ArrayList<>();
        for (Annotation annotation : annotations) {
            // custom arguments
            if (annotationToArgumentsProviderMap.containsKey(annotation.annotationType())) {
                Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> customArgumentsFunc = annotationToArgumentsProviderMap.get(annotation.annotationType());
                argumentList = customArgumentsFunc.call(field, method, declaringClass, annotation);
            }
        }
        return argumentList;
    }

    private GraphQLArgument getGraphQLArgument(GlitrArgument arg) {
        GraphQLInputType inputType = (GraphQLInputType) convertToGraphQLInputType(arg.type(), arg.name(), ImmutableMap.of(ARG_NULLABILITY, arg.nullability(), ARG_VIEW_ARGUMENT, true));

        if (arg.nullability() == GlitrArgument.NULLABILITY_NON_NULL || arg.name().equals("id")) {
            inputType = new GraphQLNonNull(inputType);
        }

        Object defaultValueInput = arg.defaultValue().equalsIgnoreCase(GlitrArgument.NO_DEFAULT_VALUE) ? null : arg.defaultValue();
        if (defaultValueInput != null && arg.type().isEnum()) {
            try {
                defaultValueInput = Enum.valueOf(arg.type(), arg.defaultValue());
            } catch (IllegalArgumentException iae) {
                defaultValueInput = null;
                logger.error("Invalid enum detected as defaultValue [" + arg.defaultValue() + "].  Defaulting to empty defaultValue.", iae);
            }
        }

        return newArgument()
                .name(arg.name())
                .description(arg.description())
                .type(inputType)
                .defaultValue(defaultValueInput)
                .build();
    }

    public GraphQLOutputType retrieveGraphQLOutputType(Class declaringClass, Method method) {
        GraphQLOutputType graphQLOutputType;
        String name = ReflectionUtil.sanitizeMethodName(method.getName());

        graphQLOutputType = getGraphQLOutputTypeFromAnnotationsOnGetter(declaringClass, method);
        graphQLOutputType = getGraphQLOutputTypeFromAnnotationsOnField(declaringClass, method, graphQLOutputType, name);

        // default OutputType
        if (graphQLOutputType == null) {
            graphQLOutputType = (GraphQLOutputType) convertToGraphQLOutputType(GenericTypeReflector.getExactReturnType(method, declaringClass), name);

            // is this an optional field
            boolean nullable = ReflectionUtil.isAnnotatedElementNullable(method);
            if (nullable) { // check the field too
                try {
                    Field field = declaringClass.getDeclaredField(name);
                    nullable = ReflectionUtil.isAnnotatedElementNullable(field);
                } catch (NoSuchFieldException e) {
                    // do nothing
                }
            }

            if (!nullable || name.equals("id")) {
                graphQLOutputType = new GraphQLNonNull(graphQLOutputType);
            }
        }

        return graphQLOutputType;
    }

    private GraphQLOutputType getGraphQLOutputTypeFromAnnotationsOnField(Class declaringClass, Method method, GraphQLOutputType graphQLOutputType, String name) {
        Field field = ReflectionUtil.getFieldByName(declaringClass, name);

        if (field != null) {
            Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
            for (Annotation annotation: fieldAnnotations) {
                // custom OutputType
                if (annotationToGraphQLOutputTypeMap.containsKey(annotation.annotationType())) {
                    Func4<Field, Method, Class, Annotation, GraphQLOutputType> customGraphQLOutputTypeFunc = annotationToGraphQLOutputTypeMap.get(annotation.annotationType());
                    GraphQLOutputType outputType = customGraphQLOutputTypeFunc.call(field, method, declaringClass, annotation);
                    if (outputType != null) {
                        graphQLOutputType = outputType;
                        break;
                    }
                }
            }
        }
        return graphQLOutputType;
    }

    private GraphQLOutputType getGraphQLOutputTypeFromAnnotationsOnGetter(Class declaringClass, Method method) {
        GraphQLOutputType graphQLOutputType = null;
        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
        for (Annotation annotation: methodAnnotations) {
            // custom OutputType
            if (annotationToGraphQLOutputTypeMap.containsKey(annotation.annotationType())) {
                Func4<Field, Method, Class, Annotation, GraphQLOutputType> customGraphQLOutputTypeFunc = annotationToGraphQLOutputTypeMap.get(annotation.annotationType());
                GraphQLOutputType outputType = customGraphQLOutputTypeFunc.call(null, method, declaringClass, annotation);
                if (outputType != null) {
                    graphQLOutputType = outputType;
                    break;
                }
            }
        }
        return graphQLOutputType;
    }

    private GraphQLList createListOutputTypeFromParametrizedType(Type type, boolean fromInterface) {
        ParameterizedType parameterizedType = (ParameterizedType)type;
        Type typeArgument = ReflectionUtil.getActualTypeArgumentFromType(parameterizedType);
        return new GraphQLList(convertToGraphQLOutputType(typeArgument, null, fromInterface));
    }

    private GraphQLList createListOutputTypeFromArrayType(Type type, boolean fromInterface) {
        Class componentType = ((Class)type).getComponentType();
        return new GraphQLList(convertToGraphQLOutputType(componentType, null, fromInterface));
    }

    private GraphQLList createListInputTypeFromParametrizedType(Type type) {
        ParameterizedType parameterizedType = (ParameterizedType)type;
        Type typeArgument = ReflectionUtil.getActualTypeArgumentFromType(parameterizedType);
        return new GraphQLList(convertToGraphQLInputType(typeArgument, null));
    }

    private GraphQLList createListInputTypeFromArrayType(Type type) {
        Class componentType = ((Class)type).getComponentType();
        return new GraphQLList(convertToGraphQLInputType(componentType, null));
    }

    public GraphQLType convertToGraphQLOutputType(Type type, String name) {
        return convertToGraphQLOutputType(type, name, false);
    }

    public GraphQLType convertToGraphQLOutputType(Type type, String name, boolean fromInterface) {
        Optional<GraphQLType> scalarType = detectScalar(type, name);
        if (scalarType.isPresent()) {
            return scalarType.get();
        }

        if (type instanceof ParameterizedType) {
            return getGraphQLTypeForOutputParameterizedType(type, name, fromInterface);
        } else if (((Class) type).isArray()) {
            // ids is also magical
            if (name != null && name.equals("ids")) {
                return new GraphQLList(GraphQLID);
            }
            return createListOutputTypeFromArrayType(type, fromInterface);
        } else if (fromInterface) { // to avoid circular references we will process the Interface field type later
            return new GraphQLTypeReference(((Class) type).getSimpleName());
        }
        return lookupOutput((Class) type);
    }

    private GraphQLType getGraphQLTypeForOutputParameterizedType(Type type, String name, boolean fromInterface) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type containerType = parameterizedType.getRawType();

        if (Collection.class.isAssignableFrom((Class)containerType)) {
            return createListOutputTypeFromParametrizedType(type, fromInterface);
        } else if (containerType == rx.Observable.class) {
            Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();
            return convertToGraphQLOutputType(fieldArgTypes[0], name, fromInterface);
        } else {
            throw new IllegalArgumentException("Unable to convert type " + type.getTypeName() + " to GraphQLOutputType");
        }
    }

    public Optional<GraphQLType> detectScalar(Type type, String name) {
        return detectScalar(type, name, null);
    }

    public Optional<GraphQLType> detectScalar(Type type, String name, Map<String, Object> args) {
        // users can register their own GraphQLScalarTypes for given Java types
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType)type).getRawType();
        }

        if (javaTypeDeclaredAsScalarMap.containsKey(type)) {
            return Optional.of(javaTypeDeclaredAsScalarMap.get(type));
        }
        // Default scalars
        // `id` keyword is magical, always return this.
        else if (name != null && name.equals("id") && Optional.ofNullable(args).map(x -> BooleanUtils.isTrue((Boolean) x.get(ARG_VIEW_ARGUMENT))).orElse(false)) {
            return Optional.of(GraphQLNonBlankID);
        } else if (name != null && name.equals("id")) {
            return Optional.of(GraphQLID);
        } else if (type == Integer.class || type == int.class) {
            return Optional.of(GraphQLInt);
        } else if (type == String.class && Optional.ofNullable(args).map(x -> ((Integer)x.get(ARG_NULLABILITY)) == GlitrArgument.NULLABILITY_NON_BLANK).orElse(false)) {
            return Optional.of(GraphQLNonBlankString);
        } else if (type == String.class) {
            return Optional.of(GraphQLString);
        } else if (type == Boolean.class || type == boolean.class) {
            return Optional.of(GraphQLBoolean);
        } else if (type == Float.class || type == Double.class) {
            return Optional.of(GraphQLFloat);
        } else if (type == Long.class) {
            return Optional.of(GraphQLLong);
        } else if (type == LocalDate.class) {
            return Optional.of(Scalars.GraphQLDate);
        } else if (type == ZonedDateTime.class || type == Instant.class) {
            return Optional.of(Scalars.GraphQLDateTime);
        } else if (type == Map.class) {
            return Optional.of(Scalars.GraphQLMap);
        }
        // not a scalar
        return Optional.empty();
    }

    public GraphQLType convertToGraphQLInputType(Type type, String name) {
        return convertToGraphQLInputType(type, name, null);
    }

    public GraphQLType convertToGraphQLInputType(Type type, String name, Map<String, Object> args) {
        Optional<GraphQLType> scalarType = detectScalar(type, name, args);
        if (scalarType.isPresent()) {
            return scalarType.get();
        }

        if (type instanceof ParameterizedType) {
            return getGraphQLTypeForInputParameterizedType(type);
        } else if (((Class) type).isArray()) {
            // ids is also magical
            if (name != null && name.equals("ids")) {
                return new GraphQLList(GraphQLID);
            }
            return createListInputTypeFromArrayType(type);
        }
        return lookupInput((Class) type);
    }

    private GraphQLType getGraphQLTypeForInputParameterizedType(Type type) {
        ParameterizedType parameterizedType = (ParameterizedType)type;
        Type containerType = parameterizedType.getRawType();

        if (Collection.class.isAssignableFrom((Class)containerType)) {
            return createListInputTypeFromParametrizedType(type);
        } else {
            throw new IllegalArgumentException("Unable to convert type " + type.getTypeName() + " to GraphQLInputType");
        }
    }

    public Map<String, GraphQLType> getNameRegistry() {
        return nameRegistry;
    }

    private static String typeName(Object input) {
        if (input == null) {
            return "null";
        }

        return input.getClass().getSimpleName();
    }
}
