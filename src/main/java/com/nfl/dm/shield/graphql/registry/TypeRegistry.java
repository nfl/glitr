package com.nfl.dm.shield.graphql.registry;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.dm.shield.graphql.domain.graph.annotation.Argument;
import com.nfl.dm.shield.graphql.domain.graph.annotation.Arguments;
import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLDescription;
import com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLIgnore;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.OverrideDataFetcher;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.batched.CompositeDataFetcherFactory;
import com.nfl.dm.shield.graphql.registry.type.Scalars;
import com.nfl.dm.shield.util.error.DataFetcherCreationException;
import graphql.relay.Relay;
import graphql.schema.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func4;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

public class TypeRegistry implements TypeResolver {

    private static final Logger logger = LoggerFactory.getLogger(TypeRegistry.class);

    private final Map<Class, GraphQLType> registry = new HashMap<>(); // map to only GraphQLOutputTypes
    private final Map<Class, GraphQLType> inputRegistry = new HashMap<>();  // map to only GraphQLInputTypes (for mutations)

    private final Map<Class, List<Object>> overrides;
    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, DataFetcher>> annotationToDataFetcherProviderMap;
    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap;
    private final Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap;
    private GraphQLInterfaceType nodeInterface;

    TypeRegistry(Map<Class, List<Object>> overrides, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, DataFetcher>> annotationToDataFetcherProviderMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, List<GraphQLArgument>>> annotationToArgumentsProviderMap, Map<Class<? extends Annotation>, Func4<Field, Method, Class, Annotation, GraphQLOutputType>> annotationToGraphQLOutputTypeMap, Relay relay) {
        this.overrides = overrides;
        this.annotationToDataFetcherProviderMap = annotationToDataFetcherProviderMap;
        this.annotationToArgumentsProviderMap = annotationToArgumentsProviderMap;
        this.annotationToGraphQLOutputTypeMap = annotationToGraphQLOutputTypeMap;
        if (relay != null) {
            this.nodeInterface = relay.nodeInterface(this);
        }
    }

    @Nullable
    public GraphQLInterfaceType getNodeInterface() {
        return nodeInterface;
    }

    public GraphQLType lookup(Class clazz) {
        if (registry.containsKey(clazz)) {
            return registry.get(clazz);
        }

        // put a type reference in while building the type to work around circular references
        registry.put(clazz, new GraphQLTypeReference(clazz.getSimpleName()));

        GraphQLOutputType type;

        if (clazz.isEnum()) {
            type = createEnumType(clazz);
        } else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            type = createInterfaceType(clazz);
        } else {
            type = createObjectType(clazz);
        }

        if (type != null) {
            registry.put(clazz, type);
        } else {
            throw new IllegalArgumentException("Unable to create GraphQLOutputType for: " + clazz.getCanonicalName());
        }

        return type;
    }

    public GraphQLType lookupInput(Class clazz) {
        if (inputRegistry.containsKey(clazz)) {
            return inputRegistry.get(clazz);
        }

        GraphQLInputType type;

        if (clazz.isEnum()) {
            type = createEnumType(clazz);
        } else {
            type = createInputObjectType(clazz);
        }

        if (type != null) {
            inputRegistry.put(clazz, type);
        } else {
            throw new IllegalArgumentException("Unable to create GraphQLInputType for: " + clazz.getCanonicalName());
        }

        return type;
    }



    /**
     * In short, the method below creates the GraphQLObjectType dynamically for pretty much any pojo class with public
     * accessors.
     *
     * In details on how, given a top level class `A`:
     *
     *   1. Extract all getter methods of `A` and store them in a map `M`
     *   2. If there are some registered overrides for `A`, for each override method in the override class
     *   check if its a getter method and add it to the methods map `M` (if not exist already)
     *   3. For each getter method in `M` lets create a graphQL field definition
     *   4. Each field definition needs a name, an output type, a data fetcher and potential arguments.
     *   (see graphQL spec for details)
     *   The way the data is fetched is dictated by the application. Given the generic nature of the method,
     *   we register a bunch of data fetchers and a single data fetcher call CompositeDataFetcher will try them one by one on each field
     *   until one works.
     *   (if the number of registered overrides grow, it might hinder performance, could we optimize that part?)
     *   5. Name and output type are deemed trivial and left as an exercise for the reader.
     *   6. Let's go first over the data fetchers:
     *   The overrideDataFetcher from the registered overrides is added first, then the overrideDataFetcher for
     *   calling the getter right away on the class is added and finally if `A` is a node (i.e has an id)
     *   the FieldViaNodeDataFetcher is added. The FieldViaNodeDataFetcher is here to expand child objects of `A`.
     *   Example: Person.address where Person is our top level class and address is of complex type Address and need to be fetched.
     *   If the top level class isn't a node then we just add finally the graphQL stock PropertyDataFetcher.
     *   7. Now the arguments:
     *   Arguments are always explicitly provided by adding @Argument on top of the getter. It the current method has it,
     *   the graphQLInputType is constructed to be later added to the field definition. In case of iterable data structures,
     *   we also look for @ForwardPagingArguments to provide paging functionality.
     *   8. All the field definitions are then added at the end to the GraphQLObjectType.
     * @param clazz top level class to be introspected
     * @return GraphQLObjectType object exposed via graphQL.
     */
    private GraphQLObjectType createObjectType(Class clazz) {
        boolean isNode = Arrays.stream(clazz.getMethods()).anyMatch(method -> method.getName().equals("getId"));
        Map<String, Pair<Method, Class>> methods = getMethodMap(clazz);

        if (overrides.containsKey(clazz)) {
            for (Object override : overrides.get(clazz)) {
                for (Method method : override.getClass().getMethods()) {
                    if (!eligibleMethod(method)) {
                        continue;
                    }
                    methods.putIfAbsent(method.getName(), Pair.of(method, override.getClass()));
                }
            }
        }

        List<GraphQLFieldDefinition> fields = methods.values().stream()
                .map(pair -> {
                    Method method = pair.getLeft();
                    Class declaringClass = pair.getRight();

                    String name = sanitizeMethodName(method.getName());

                    // 1. Fetchers
                    // 2. Arguments
                    // 3. OutputType
                    List<DataFetcher> fetchers = new ArrayList<>();
                    List<GraphQLArgument> arguments = new ArrayList<>();
                    GraphQLOutputType graphQLOutputType = null;

                    // Override Fetchers
                    if (overrides.containsKey(clazz)) {
                        for (Object override : overrides.get(clazz)) {
                            fetchers.add(new OverrideDataFetcher(name, override));
                        }
                    }

                    // we add a default OverrideDataFetcher for override getters in the actual class itself
                    fetchers.add(new OverrideDataFetcher(name, clazz));

                    // A. inspect annotations on getter
                    Annotation[] methodAnnotations = method.getDeclaredAnnotations();
                    for (Annotation annotation: methodAnnotations) {
                        // Custom Fetchers
                        if (annotationToDataFetcherProviderMap.containsKey(annotation.annotationType())) {
                            Func4<Field, Method, Class, Annotation, DataFetcher> customDataFetcherFunc = annotationToDataFetcherProviderMap.get(annotation.annotationType());
                            DataFetcher dataFetcher = customDataFetcherFunc.call(null, method, declaringClass, annotation);
                            if (dataFetcher == null) {
                                continue;
                            }
                            fetchers.add(dataFetcher);
                        }

                        // Custom Arguments
                        if (annotationToArgumentsProviderMap.containsKey(annotation.annotationType())) {
                            Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> customArgumentsFunc = annotationToArgumentsProviderMap.get(annotation.annotationType());
                            List<GraphQLArgument> argumentList = customArgumentsFunc.call(null, method, declaringClass, annotation);
                            arguments.addAll(argumentList);
                        }

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
                            // Custom Fetchers
                            if (annotationToDataFetcherProviderMap.containsKey(annotation.annotationType())) {
                                Func4<Field, Method, Class, Annotation, DataFetcher> customDataFetcherFunc = annotationToDataFetcherProviderMap.get(annotation.annotationType());
                                DataFetcher dataFetcher = customDataFetcherFunc.call(field, method, declaringClass, annotation);
                                if (dataFetcher == null) {
                                    continue;
                                }
                                fetchers.add(dataFetcher);
                            }

                            // Custom Arguments
                            if (annotationToArgumentsProviderMap.containsKey(annotation.annotationType())) {
                                Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> customArgumentsFunc = annotationToArgumentsProviderMap.get(annotation.annotationType());
                                List<GraphQLArgument> argumentList = customArgumentsFunc.call(field, method, declaringClass, annotation);
                                arguments.addAll(argumentList);
                            }

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

                    // default fetcher
                    fetchers.add(new PropertyDataFetcher(name));

                    // Get back a batched or un-batched CompositeDataFetcher depending on the list
                    DataFetcher dataFetcher = null;
                    try {
                        dataFetcher = CompositeDataFetcherFactory.create(fetchers);
                    } catch (IllegalArgumentException e) {
                        throw new DataFetcherCreationException(e.getMessage() + " For " + declaringClass +"."+ name);
                    }

                    // default argument, argument annotation can be found on either the method or field but method level args take precedence
                    Argument[] annotatedArguments;
                    Argument[] annotatedGetterArguments = getArgumentsFromMethod(method);
                    Argument[] annotatedFieldArguments = getArgumentsFromField(field);
                    if (annotatedGetterArguments.length == 0) {
                        annotatedArguments = annotatedFieldArguments;
                    } else {
                        annotatedArguments = annotatedGetterArguments;
                    }

                    if (annotatedGetterArguments.length > 0 && annotatedFieldArguments.length > 0) {
                        logger.warn("Both Method and Field level @Argument(s) found, ignoring annotations [{}] for Field [{}]", annotatedFieldArguments, field.getName());
                    }

                    arguments.addAll(Arrays.stream(annotatedArguments)
                            .map(arg -> {
                                GraphQLInputType inputType = (GraphQLInputType) convertToGraphQLOutputType(arg.type(), arg.name());

                                if (!arg.nullable() || arg.name().equals("id")) {
                                    inputType = new GraphQLNonNull(inputType);
                                }

                                return newArgument()
                                        .name(arg.name())
                                        .type(inputType)
                                        .build();
                            })
                            .collect(Collectors.toList()));

                    // default outputType
                    if (graphQLOutputType == null) {
                        graphQLOutputType = (GraphQLOutputType) convertToGraphQLOutputType(
                                GenericTypeReflector.getExactReturnType(method, declaringClass), name);

                        // nullable
                        boolean nullable = !method.isAnnotationPresent(com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull.class);

                        if (!nullable || name.equals("id")) {
                            graphQLOutputType = new GraphQLNonNull(graphQLOutputType);
                        }
                    }

                    // description
                    String description = method.isAnnotationPresent(GraphQLDescription.class) ? method.getAnnotation(GraphQLDescription.class).value() : GraphQLDescription.DEFAULT_DESCRIPTION;

                    return newFieldDefinition()
                            .name(name)
                            .description(description)
                            .dataFetcher(dataFetcher)
                            .type(graphQLOutputType)
                            .argument(arguments)
                            .build();
                })
                .collect(Collectors.toList());

        if (fields.size() == 0) {
            // GraphiQL doesn't like objects with no fields, so add an unused field to be safe.
            fields.add(newFieldDefinition().name("unused_fields_dead_object").type(GraphQLBoolean).staticValue(false).build());
        }

        // description
        String description = clazz.isAnnotationPresent(GraphQLDescription.class) ? ((GraphQLDescription)clazz.getAnnotation(GraphQLDescription.class)).value() : GraphQLDescription.DEFAULT_DESCRIPTION;

        GraphQLObjectType.Builder builder = newObject()
                .name(clazz.getSimpleName())
                .description(description)
                .fields(fields);

        if (nodeInterface != null && isNode) {
            builder.withInterface(nodeInterface);
        }

        return builder.build();
    }


    public GraphQLInputObjectType createInputObjectType(Class clazz) {
        Map<String, Pair<Method, Class>> methods = getMethodMap(clazz);

        List<GraphQLInputObjectField> fields = methods.values().stream()
                .map(pair -> {
                    Method method = pair.getLeft();
                    Class declaringClass = pair.getRight();

                    String name = sanitizeMethodName(method.getName());

                    // TYPE
                    GraphQLInputType graphQLInputType = (GraphQLInputType) convertToGraphQLInputType(GenericTypeReflector.getExactReturnType(method, declaringClass), name);

                    // description
                    String description = method.isAnnotationPresent(GraphQLDescription.class) ? method.getAnnotation(GraphQLDescription.class).value() : GraphQLDescription.DEFAULT_DESCRIPTION;

                    // nullable
                    boolean nullable = !method.isAnnotationPresent(com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull.class);

                    if (!nullable || name.equals("id")) {
                        graphQLInputType = new GraphQLNonNull(graphQLInputType);
                    }

                    return newInputObjectField()
                            .type(graphQLInputType)
                            .name(name)
                            .description(description)
                            .build();
                })
                .collect(Collectors.toList());

        GraphQLInputObjectType.Builder builder = newInputObject()
                .name(StringUtils.uncapitalize(clazz.getSimpleName()))
                .fields(fields);

        // description
        if (clazz.isAnnotationPresent(GraphQLDescription.class)) {
            builder.description(((GraphQLDescription)clazz.getAnnotation(GraphQLDescription.class)).value());
        }

        return builder.build();
    }

    private Map<String, Pair<Method, Class>> getMethodMap(Class clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(this::eligibleMethod)
                .collect(Collectors.toMap(Method::getName, y -> Pair.of(y, y.getDeclaringClass())));
    }

    /**
     * See createObjectType comments but DataFetchers are not needed for interfaces.
     *
     * @param clazz interface class
     * @return GraphQLInterfaceType with a type resolver.
     */
    private GraphQLInterfaceType createInterfaceType(Class clazz) {
        List<GraphQLFieldDefinition> fields = Arrays.stream(clazz.getDeclaredMethods())
                .filter(this::eligibleMethod)
                .map(method -> {
                    String name = sanitizeMethodName(method.getName());

                    // description
                    String description = method.isAnnotationPresent(GraphQLDescription.class) ? method.getAnnotation(GraphQLDescription.class).value() : GraphQLDescription.DEFAULT_DESCRIPTION;

                    // type
                    GraphQLType type = convertToGraphQLOutputType(GenericTypeReflector.getExactReturnType(method, clazz), null);

                    // nullable
                    boolean nullable = !method.isAnnotationPresent(com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull.class);

                    if (!nullable || name.equals("id")) {
                        type = new GraphQLNonNull(type);
                    }

                    return newFieldDefinition()
                            .name(name)
                            .description(description)
                            .type((GraphQLOutputType) type)
                            .build();
                })
                .collect(Collectors.toList());

        // description
        String description = clazz.isAnnotationPresent(GraphQLDescription.class) ? ((GraphQLDescription)clazz.getAnnotation(GraphQLDescription.class)).value() : GraphQLDescription.DEFAULT_DESCRIPTION;

        return newInterface()
                .name(clazz.getSimpleName())
                .description(description)
                .typeResolver(object -> (GraphQLObjectType) lookup(object.getClass()))
                .fields(fields)
                .build();
    }

    private GraphQLList createListOutputTypeFromParametrizedType(Type type) {
        ParameterizedType parameterizedType = (ParameterizedType)type;

        Type typeArgument = getActualTypeArgumentFromType(parameterizedType);

        return new GraphQLList(convertToGraphQLOutputType(typeArgument, null));
    }

    private GraphQLList createListOutputTypeFromArrayType(Type type) {

        Class componentType = ((Class)type).getComponentType();

        return new GraphQLList(convertToGraphQLOutputType(componentType, null));
    }

    private GraphQLList createListInputTypeFromParametrizedType(Type type) {
        ParameterizedType parameterizedType = (ParameterizedType)type;

        Type typeArgument = getActualTypeArgumentFromType(parameterizedType);

        return new GraphQLList(convertToGraphQLInputType(typeArgument, null));
    }

    private GraphQLList createListInputTypeFromArrayType(Type type) {

        Class componentType = ((Class)type).getComponentType();

        return new GraphQLList(convertToGraphQLInputType(componentType, null));
    }

    private GraphQLEnumType createEnumType(Class clazz) {
        GraphQLEnumType.Builder builder = newEnum().name(clazz.getSimpleName());

        for (Object constant : clazz.getEnumConstants()) {
            builder.value(constant.toString(), constant);
        }

        return builder.build();
    }

    public GraphQLType convertToGraphQLOutputType(Type type, String name) {
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
                return createListOutputTypeFromParametrizedType(type);
            } else if (containerType == rx.Observable.class) {
                Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();

                return convertToGraphQLOutputType(fieldArgTypes[0], name);
            } else {
                throw new IllegalArgumentException("Unable to convert type " + type.getTypeName() + " to GraphQLOutputType");
            }
        } else if (((Class) type).isArray()) {
            return createListOutputTypeFromArrayType(type);
        }

        return lookup((Class) type);
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

    public static String sanitizeMethodName(String name) {
        return StringUtils.uncapitalize(
                name.startsWith("is")
                        ? name.substring(2)
                        : name.substring(3));
    }

    private Boolean eligibleMethod(Method method) {
        Annotation graphQLIgnore = method.getAnnotation(GraphQLIgnore.class);
        if (graphQLIgnore != null) {
            return false;
        }

        String methodName = method.getName();
        String fieldName = sanitizeMethodName(methodName);

        Field field;
        try {
            field = method.getDeclaringClass().getDeclaredField(fieldName);
            Annotation fieldAnnotations = field.getAnnotation(GraphQLIgnore.class);
            if (fieldAnnotations != null) {
                return false;
            }
        } catch (NoSuchFieldException e) {
            // thats fine
            logger.debug("Field not found: {}", e);
        }

        return (methodName.startsWith("is") || methodName.startsWith("get"))
                && method.getDeclaringClass() != Object.class
                && (!Map.class.isAssignableFrom(method.getReturnType()));
    }

    public static Type getActualTypeArgumentFromType(ParameterizedType parameterizedType) {
        Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();

        if (fieldArgTypes.length > 1) {
            throw new IllegalArgumentException("Type can only have one generic argument: " + parameterizedType);
        }

        return fieldArgTypes[0];
    }

    public static Argument[] getArgumentsFromMethod(Method method) {
        Argument[] singleAnnotationArguments = method.isAnnotationPresent(Argument.class) ? method.getAnnotationsByType(Argument.class) : new Argument[0];
        Argument[] groupedAnnotationArguments = method.isAnnotationPresent(Arguments.class) ? method.getAnnotation(Arguments.class).value() : new Argument[0];
        return ArrayUtils.addAll(singleAnnotationArguments, groupedAnnotationArguments);
    }

    public static Argument[] getArgumentsFromField(Field field) {
        if (field == null) {
            return new Argument[0];
        }
        Argument[] singleAnnotationArguments = field.isAnnotationPresent(Argument.class) ? field.getAnnotationsByType(Argument.class) : new Argument[0];
        Argument[] groupedAnnotationArguments = field.isAnnotationPresent(Arguments.class) ? field.getAnnotation(Arguments.class).value() : new Argument[0];
        return ArrayUtils.addAll(singleAnnotationArguments, groupedAnnotationArguments);
    }

    @Override
    public GraphQLObjectType getType(Object object) {
        return (GraphQLObjectType)registry.get(object.getClass());
    }
}
