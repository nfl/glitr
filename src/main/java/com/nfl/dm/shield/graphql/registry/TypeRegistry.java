package com.nfl.dm.shield.graphql.registry;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.dm.shield.graphql.ReflectionUtil;
import com.nfl.dm.shield.graphql.domain.graph.annotation.Argument;
import com.nfl.dm.shield.graphql.registry.datafetcher.AnnotationBasedDataFetcherFactory;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.OverrideDataFetcher;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.batched.CompositeDataFetcherFactory;
import com.nfl.dm.shield.graphql.registry.type.*;
import com.nfl.dm.shield.graphql.registry.type.Scalars;
import com.nfl.dm.shield.graphql.exception.GlitrDataFetcherCreationException;
import graphql.relay.Relay;
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

public class TypeRegistry implements TypeResolver {

    private static final Logger logger = LoggerFactory.getLogger(TypeRegistry.class);

    private final Map<Class, GraphQLType> registry = new ConcurrentHashMap<>();

    private final Map<Class, List<Object>> overrides;
    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap;
    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap;
    private final Map<Class<? extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap;

    private GraphQLInterfaceType nodeInterface;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private Relay relay;

    private GraphQLTypeFactory graphQLTypeFactory = new GraphQLTypeFactory()
            .withOutputTypeFactory(new GraphQLEnumTypeFactory(), JavaType.ENUM)
            .withOutputTypeFactory(new GraphQLInterfaceTypeFactory(this), JavaType.INTERFACE)
            .withOutputTypeFactory(new GraphQLInterfaceTypeFactory(this), JavaType.ABSTRACT_CLASS)
            .withOutputTypeFactory(new GraphQLObjectTypeFactory(this), JavaType.CLASS)
            .withInputTypeFactory(new GraphQLInputObjectTypeFactory(this), JavaType.ABSTRACT_CLASS, JavaType.CLASS, JavaType.INTERFACE);


    TypeRegistry(Map<Class, List<Object>> overrides, Map<Class<?extends Annotation>, AnnotationBasedDataFetcherFactory> annotationToDataFetcherFactoryMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap, Relay relay) {
        this.overrides = overrides;
        this.annotationToDataFetcherFactoryMap = annotationToDataFetcherFactoryMap;
        this.annotationToArgumentsProviderMap = annotationToArgumentsProviderMap;
        this.annotationToGraphQLOutputTypeMap = annotationToGraphQLOutputTypeMap;
        this.relay = relay;
        if (relay != null) {
            this.nodeInterface = relay.nodeInterface(this);
        }
    }

    /**
     * Type Dictionary is used by GraphQLSchema to provide additional types necessary for the type collection
     * https://github.com/graphql-java/graphql-java/commit/6668d1a7e9279d02d499e36b379475d67b6c57b9
     * @return set of GraphQLType to be passed to GraphQLSchema
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

    /**
     * Root class should be passed here so the graph can be inspected in its entirety
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

    public GraphQLType lookupInput(Class clazz) {
        if (registry.containsKey(clazz)) {
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
                .map(pair -> {
                    Method method = pair.getLeft();
                    Class declaringClass = pair.getRight();

                    // 1. GraphQL Field Name
                    String name = ReflectionUtil.sanitizeMethodName(method.getName());
                    // 2. DataFetcher
                    List<DataFetcher> fetchers = retrieveDataFetchers(clazz, declaringClass, method);
                    DataFetcher dataFetcher = createDataFetchersFromDataFetcherList(fetchers, declaringClass, name);
                    // 3. Arguments
                    GraphQLArgument argument = createRelayInputArgument(declaringClass, method);
                    // 4. OutputType
                    GraphQLOutputType graphQLOutputType = retrieveGraphQLOutputType(declaringClass, method);
                    // 5. Description
                    String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);
                    // 6. //TODO: static value
                    // 7. //TODO: deprecation reason

                    return newFieldDefinition()
                            .name(name)
                            .description(description)
                            .type(graphQLOutputType)
                            .argument(argument)
                            .dataFetcher(dataFetcher)
                            .build();
                })
                .collect(Collectors.toList());

        if (fields.size() == 0) {
            // GraphiQL doesn't like objects with no fields, so add an unused field to be safe.
            fields.add(newFieldDefinition().name("unused_fields_dead_object").type(GraphQLBoolean).staticValue(false).build());
        }

        // description
        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(clazz);

        GraphQLObjectType.Builder builder = newObject()
                .name(clazz.getSimpleName())
                .description(description)
                .fields(fields);

        return builder.build();
    }

    public static DataFetcher createDataFetchersFromDataFetcherList(List<DataFetcher> fetchers, Class declaringClass, String name) {
        try {
            return CompositeDataFetcherFactory.create(fetchers);
        } catch (IllegalArgumentException e) {
            throw new GlitrDataFetcherCreationException(e.getMessage() + " For " + declaringClass +"."+ name);
        }
    }

    private GraphQLArgument createRelayInputArgument(Class methodDeclaringClass, Method method) {
        Argument[] annotatedGetterArguments = ReflectionUtil.getArgumentsFromMethod(method);
        if (annotatedGetterArguments.length != 1) {
            throw new IllegalArgumentException("Only one @Argument annotation can be placed on a relay mutation for class "+methodDeclaringClass.getSimpleName()+" and method "+ method.getName());
        }

        Argument arg = annotatedGetterArguments[0];
        if (!arg.name().equals("input")) {
            throw new IllegalArgumentException("@Argument annotation name must be `input` for class "+methodDeclaringClass.getSimpleName()+" and method "+ method.getName());
        }

        GraphQLInputType inputType = (GraphQLInputType) convertToGraphQLInputType(arg.type(), arg.name());

        if (!arg.nullable()) {
            inputType = new GraphQLNonNull(inputType);
        }

        return newArgument()
                .name(arg.name())
                .description(arg.description())
                .type(inputType)
                .defaultValue(arg.defaultValue())
                .build();
    }

    /**
     * We allow defining GraphQL additional fields outside of the inspected class.
     * In fact, if the getter-method name conflicts with an existing method defined in the inspected class, it won't be added.
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

        // Override Fetchers
        if (overrides.containsKey(clazz)) {
            for (Object override : overrides.get(clazz)) {
                fetchers.add(new OverrideDataFetcher(name, override));
            }
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
        // A. inspect annotations on getter
        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
        for (Annotation annotation: methodAnnotations) {
            // Custom Fetchers
            if (annotationToDataFetcherFactoryMap.containsKey(annotation.annotationType())) {
                AnnotationBasedDataFetcherFactory annotationBasedDataFetcherFactory = annotationToDataFetcherFactoryMap.get(annotation.annotationType());
                DataFetcher dataFetcher = annotationBasedDataFetcherFactory.create(null, method, declaringClass, annotation);
                if (dataFetcher != null) {
                    return dataFetcher;
                }
            }
        }

        // B. inspect annotations on field of same name.
        Field field;
        try {
            field = declaringClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // that's fine
            logger.debug("Field not found: {} for class {} ", fieldName, declaringClass, e);
            return null;
        }

        if (field != null) {
            Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
            for (Annotation annotation: fieldAnnotations) {
                // Custom Fetchers
                if (annotationToDataFetcherFactoryMap.containsKey(annotation.annotationType())) {
                    AnnotationBasedDataFetcherFactory annotationBasedDataFetcherFactory = annotationToDataFetcherFactoryMap.get(annotation.annotationType());
                    DataFetcher dataFetcher = annotationBasedDataFetcherFactory.create(field, method, declaringClass, annotation);
                    if (dataFetcher != null) {
                        return dataFetcher;
                    }
                }
            }
        }

        //No custom annotations found.
        return null;
    }

    public List<GraphQLArgument> retrieveArguments(Class declaringClass, Method method) {
        List<GraphQLArgument> arguments = new ArrayList<>();
        String name = ReflectionUtil.sanitizeMethodName(method.getName());

        // A. inspect annotations on getter
        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
        for (Annotation annotation: methodAnnotations) {
            // Custom Arguments
            if (annotationToArgumentsProviderMap.containsKey(annotation.annotationType())) {
                Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> customArgumentsFunc = annotationToArgumentsProviderMap.get(annotation.annotationType());
                List<GraphQLArgument> argumentList = customArgumentsFunc.call(null, method, declaringClass, annotation);
                arguments.addAll(argumentList);
            }
        }

        // B. inspect annotations on field of same name.
        Field field = null;
        try {
            field = declaringClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            // that's fine
            logger.debug("Field not found: {} for class {} ", name, declaringClass, e);
        }

        if (field != null) {
            Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
            for (Annotation annotation: fieldAnnotations) {
                // Custom Arguments
                if (annotationToArgumentsProviderMap.containsKey(annotation.annotationType())) {
                    Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> customArgumentsFunc = annotationToArgumentsProviderMap.get(annotation.annotationType());
                    List<GraphQLArgument> argumentList = customArgumentsFunc.call(field, method, declaringClass, annotation);
                    arguments.addAll(argumentList);
                }
            }
        }

        // default argument, argument annotation can be found on either the method or field but method level args take precedence
        Argument[] annotatedArguments;
        Argument[] annotatedGetterArguments = ReflectionUtil.getArgumentsFromMethod(method);
        Argument[] annotatedFieldArguments = ReflectionUtil.getArgumentsFromField(field);

        if (annotatedGetterArguments.length == 0) {
            annotatedArguments = annotatedFieldArguments;
        } else {
            annotatedArguments = annotatedGetterArguments;
        }

        if (annotatedGetterArguments.length > 0 && annotatedFieldArguments.length > 0) {
            String fieldName = (field != null) ? field.getName() : "null";
            logger.warn("Both Method and Field level @Argument(s) found, ignoring annotations [{}] for Field [{}]", annotatedFieldArguments, fieldName);
        }

        arguments.addAll(Arrays.stream(annotatedArguments)
                .map(arg -> {
                    GraphQLInputType inputType = (GraphQLInputType) convertToGraphQLInputType(arg.type(), arg.name());

                    if (!arg.nullable() || arg.name().equals("id")) {
                        inputType = new GraphQLNonNull(inputType);
                    }

                    return newArgument()
                            .name(arg.name())
                            .description(arg.description())
                            .type(inputType)
                            .build();
                })
                .collect(Collectors.toList()));

        return arguments;
    }

    public GraphQLOutputType retrieveGraphQLOutputType(Class declaringClass, Method method) {
        GraphQLOutputType graphQLOutputType = null;
        String name = ReflectionUtil.sanitizeMethodName(method.getName());

        // A. inspect annotations on getter
        Annotation[] methodAnnotations = method.getDeclaredAnnotations();
        for (Annotation annotation: methodAnnotations) {
            // Custom OutputType
            if (annotationToGraphQLOutputTypeMap.containsKey(annotation.annotationType())) {
                Func4<Field, Method, Class, Annotation, GraphQLOutputType> customGraphQLOutputTypeFunc = annotationToGraphQLOutputTypeMap.get(annotation.annotationType());
                GraphQLOutputType outputType = customGraphQLOutputTypeFunc.call(null, method, declaringClass, annotation);
                if (outputType != null) {
                    graphQLOutputType = outputType;
                    break;
                }
            }
        }

        // B. inspect annotations on field of same name.
        Field field = null;
        try {
            field = declaringClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            // that's fine
            logger.debug("Field not found: {} for class {} ", name, declaringClass, e);
        }

        if (field != null) {
            Annotation[] fieldAnnotations = field.getDeclaredAnnotations();
            for (Annotation annotation: fieldAnnotations) {
                // Custom OutputType
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

        // default outputType
        if (graphQLOutputType == null) {
            graphQLOutputType = (GraphQLOutputType) convertToGraphQLOutputType(GenericTypeReflector.getExactReturnType(method, declaringClass), name);

            // nullable
            boolean nullable = ReflectionUtil.isAnnotatedElementNullable(method);

            if (!nullable || name.equals("id")) {
                graphQLOutputType = new GraphQLNonNull(graphQLOutputType);
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
            ParameterizedType parameterizedType = (ParameterizedType)type;
            Type containerType = parameterizedType.getRawType();

            if (Collection.class.isAssignableFrom((Class)containerType)) {
                return createListOutputTypeFromParametrizedType(type, fromInterface);
            } else if (containerType == rx.Observable.class) {
                Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();

                return convertToGraphQLOutputType(fieldArgTypes[0], name, fromInterface);
            } else {
                throw new IllegalArgumentException("Unable to convert type " + type.getTypeName() + " to GraphQLOutputType");
            }
        } else if (((Class) type).isArray()) {
            return createListOutputTypeFromArrayType(type, fromInterface);
        } else if (fromInterface) { // to avoid circular references we will process the Interface field type later
            return new GraphQLTypeReference(((Class) type).getSimpleName());
        }

        return lookupOutput((Class) type);
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
            ParameterizedType parameterizedType = (ParameterizedType)type;
            Type containerType = parameterizedType.getRawType();

            if (Collection.class.isAssignableFrom((Class)containerType)) {
                return createListInputTypeFromParametrizedType(type);
            } else {
                throw new IllegalArgumentException("Unable to convert type " + type.getTypeName() + " to GraphQLInputType");
            }
        } else if (((Class) type).isArray()) {
            return createListInputTypeFromArrayType(type);
        }

        return lookupInput((Class) type);
    }
}
