package com.nfl.dm.shield.graphql.registry.type;

import com.nfl.dm.shield.graphql.ReflectionUtil;
import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import graphql.schema.*;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static com.nfl.dm.shield.graphql.registry.TypeRegistry.createDataFetchersFromDataFetcherList;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphQLObjectTypeFactory implements DelegateTypeFactory {

    private final TypeRegistry typeRegistry;

    public GraphQLObjectTypeFactory(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    @Override
    public GraphQLOutputType create(Class clazz) {
        return createObjectType(clazz);
    }


    /**
     * The method below creates the GraphQLObjectType dynamically for pretty much any pojo class with public
     * accessors.
     *
     * In details on how, given a top level class `A`:
     *
     *   1. Extract all getter methods of `A` and store them in a map `M`
     *   2. If there are some registered overrides for `A`, for each override method in the override class
     *   check if its a getter method and add it to the methods map `M` (if does not exist already)
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
        Map<String, Pair<Method, Class>> methods = ReflectionUtil.getMethodMap(clazz);

        // add extra methods from outside the inspected class coming from an override object
        typeRegistry.addExtraMethodsToTheSchema(clazz, methods);

        List<GraphQLFieldDefinition> fields = methods.values().stream()
                .map(pair -> {
                    Method method = pair.getLeft();
                    Class declaringClass = pair.getRight();

                    // 1. GraphQL Field Name
                    String name = ReflectionUtil.sanitizeMethodName(method.getName());
                    // 2. DataFetcher
                    List<DataFetcher> fetchers = typeRegistry.retrieveDataFetchers(clazz, declaringClass, method);
                    DataFetcher dataFetcher = createDataFetchersFromDataFetcherList(fetchers, declaringClass, name);
                    // 3. Arguments
                    List<GraphQLArgument> arguments = typeRegistry.retrieveArguments(declaringClass, method);
                    // 4. OutputType
                    GraphQLOutputType graphQLOutputType = typeRegistry.retrieveGraphQLOutputType(declaringClass, method);
                    // 5. Description
                    String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);
                    // 6. //TODO: static value
                    // 7. //TODO: deprecation reason

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
        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(clazz);

        // implemented interfaces
        List<GraphQLInterfaceType> graphQLInterfaceTypes = retrieveInterfacesForType(clazz);

        // extended abstract classes
        List<GraphQLInterfaceType> graphQLAbstractClassTypes = retrieveAbstractClassesForType(clazz);
        graphQLInterfaceTypes.addAll(graphQLAbstractClassTypes);

        GraphQLObjectType.Builder builder = newObject()
                .name(clazz.getSimpleName())
                .description(description)
                .withInterfaces(graphQLInterfaceTypes.toArray(new GraphQLInterfaceType[graphQLInterfaceTypes.size()]))
                .fields(fields);

        // relay is enabled, add Node interface implementation if one of the eligible methods is named getId
        if (typeRegistry.getNodeInterface() != null && methods.keySet().stream().anyMatch(name -> name.equals("getId"))) {
            builder.withInterface(typeRegistry.getNodeInterface());
        }

        return builder.build();
    }

    /**
     * Look up and inspect abstract class extended by a specific class.
     * @param clazz being inspected
     * @return list of GraphQLInterfaceType
     */
    public List<GraphQLInterfaceType> retrieveAbstractClassesForType(Class clazz) {
        List<GraphQLInterfaceType> abstractClasses = new ArrayList<>();

        LinkedList<Class> queue = new LinkedList<>();
        queue.add(clazz);

        while (!queue.isEmpty()) {
            Class aClass = queue.poll().getSuperclass();
            if (aClass == null) {
                continue;
            }
            if (Modifier.isAbstract(aClass.getModifiers())) {
                abstractClasses.add((GraphQLInterfaceType) typeRegistry.lookupOutput(aClass));
                queue.add(aClass);
            }
        }

        return abstractClasses;
    }

    /**
     * Look up and inspect interfaces implemented by a specific class.
     * @param clazz being inspected
     * @return list of GraphQLInterfaceType
     */
    public List<GraphQLInterfaceType> retrieveInterfacesForType(Class clazz) {
        List<GraphQLInterfaceType>  interfaceTypes = new ArrayList<>();

        LinkedList<Class> queue = new LinkedList<>();
        queue.add(clazz);

        while (!queue.isEmpty()) {
            Class aClass = queue.poll();
            Class<?>[] interfacesForClass = aClass.getInterfaces();
            queue.addAll(Arrays.asList(interfacesForClass));
            for (Class interfaceClass: interfacesForClass) {
                GraphQLInterfaceType graphQLInterfaceType = (GraphQLInterfaceType) typeRegistry.lookupOutput(interfaceClass);
                interfaceTypes.add(graphQLInterfaceType);
            }
        }

        return interfaceTypes;
    }
}
