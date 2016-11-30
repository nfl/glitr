package com.nfl.glitr.registry;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.registry.type.*;
import com.nfl.glitr.util.ReflectionUtil;
import com.nfl.glitr.annotation.GlitrArgument;
import com.nfl.glitr.exception.GlitrException;
import com.nfl.glitr.registry.datafetcher.AnnotationBasedDataFetcherFactory;
import com.nfl.glitr.registry.datafetcher.query.OverrideDataFetcher;
import com.nfl.glitr.registry.datafetcher.query.batched.CompositeDataFetcherFactory;
import com.nfl.glitr.relay.Relay;
import graphql.schema.*;
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

    private final Map<Class, GraphQLType> registry = new ConcurrentHashMap<>();
    private final Map<Class, List<Object>> overrides;

    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap;
    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap;
    private final Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap;
    private final Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap;

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


    TypeRegistry(Map<Class, List<Object>> overrides, Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap, Map<Class<? extends Annotation>, DataFetcher> annotationToDataFetcherMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap, Relay relay, boolean explicitRelayNodeScanEnabled) {
        this.overrides = overrides;
        this.annotationToDataFetcherFactoryMap = annotationToDataFetcherFactoryMap;
        this.annotationToDataFetcherMap = annotationToDataFetcherMap;
        this.annotationToArgumentsProviderMap = annotationToArgumentsProviderMap;
        this.annotationToGraphQLOutputTypeMap = annotationToGraphQLOutputTypeMap;
        this.relay = relay;
        if (relay != null) {
            this.nodeInterface = relay.nodeInterface(this);
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
    public GraphQLObjectType getType(Object object) {
        return (GraphQLObjectType)registry.get(object.getClass());
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
        }

        // put a type reference in while building the type to work around circular references
        registry.put(clazz, new GraphQLTypeReference(clazz.getSimpleName()));

        GraphQLOutputType type = graphQLTypeFactory.createGraphQLOutputType(clazz);

        if (type != null) {
            registry.put(clazz, type);
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
        }

        GraphQLInputType type = graphQLTypeFactory.createGraphQLInputType(clazz);

        if (type != null) {
            registry.put(clazz, type);
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

        return newFieldDefinition()
                .name(name)
                .description(ReflectionUtil.getDescriptionFromAnnotatedElement(method))
                .type(retrieveGraphQLOutputType(declaringClass, method))
                .argument(createRelayInputArgument(declaringClass, method))
                .dataFetcher(dataFetcher)
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
        if (!arg.nullable()) {
            inputType = new GraphQLNonNull(inputType);
        }

        // Default values need to match type so we replace our default String with null
        Object defaultValue = null;
        if (!arg.defaultValue().equalsIgnoreCase("No Default Value")) {
            defaultValue = arg.defaultValue();
        }

        return newArgument()
                .name(arg.name())
                .description(arg.description())
                .type(inputType)
                .defaultValue(defaultValue)
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
        Field field = getFieldByName(declaringClass, fieldName);
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
                } else if(annotationToDataFetcherMap.containsKey(annotationType)) {
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
            } else if(annotationToDataFetcherMap.containsKey(annotationType)) {
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
        Field field = getFieldByName(declaringClass, name);
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
        GraphQLInputType inputType = (GraphQLInputType) convertToGraphQLInputType(arg.type(), arg.name());

        if (!arg.nullable() || arg.name().equals("id")) {
            inputType = new GraphQLNonNull(inputType);
        }

        return newArgument()
                .name(arg.name())
                .description(arg.description())
                .type(inputType)
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

            if (!nullable || name.equals("id")) {
                graphQLOutputType = new GraphQLNonNull(graphQLOutputType);
            }
        }

        return graphQLOutputType;
    }

    private GraphQLOutputType getGraphQLOutputTypeFromAnnotationsOnField(Class declaringClass, Method method, GraphQLOutputType graphQLOutputType, String name) {
        Field field = getFieldByName(declaringClass, name);

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

    private Field getFieldByName(Class declaringClass, String name) {
        Field field = null;
        try {
            field = declaringClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            // that's fine
            logger.debug("Field not found: {} for class {} ", name, declaringClass);
        }
        return field;
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
        // id is magical, always return this.
        if (name != null && name.equals("id")) {
            return GraphQLID;
        } else if (type == Integer.class || type == int.class) {
            return GraphQLInt;
        } else if (type == String.class) {
            return GraphQLString;
        } else if (type == Boolean.class || type == boolean.class) {
            return GraphQLBoolean;
        } else if (type == Float.class || type == Double.class) {
            return GraphQLFloat;
        } else if (type == Long.class) {
            return GraphQLLong;
        } else if (type == LocalDate.class) {
            return Scalars.GraphQLDate;
        } else if (type == ZonedDateTime.class || type == Instant.class) {
            return Scalars.GraphQLDateTime;
        } else if (type instanceof ParameterizedType) {
            return getGraphQLTypeForOutputParameterizedType(type, name, fromInterface);
        } else if (((Class) type).isArray()) {
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

    public GraphQLType convertToGraphQLInputType(Type type, String name) {
        // id is magical, always return this.
        if (name != null && name.equals("id")) {
            return GraphQLID;
        } else if (type == Integer.class || type == int.class) {
            return GraphQLInt;
        } else if (type == String.class) {
            return GraphQLString;
        } else if (type == Boolean.class || type == boolean.class) {
            return GraphQLBoolean;
        } else if (type == Float.class || type == Double.class) {
            return GraphQLFloat;
        } else if (type == Long.class) {
            return GraphQLLong;
        } else if (type == LocalDate.class) {
            return Scalars.GraphQLDate;
        } else if (type == ZonedDateTime.class || type == Instant.class) {
            return Scalars.GraphQLDateTime;
        } else if (type instanceof ParameterizedType) {
            return getGraphQLTypeForInputParameterizedType(type);
        } else if (((Class) type).isArray()) {
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
}
