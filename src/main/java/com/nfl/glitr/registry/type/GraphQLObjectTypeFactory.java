package com.nfl.glitr.registry.type;

import com.nfl.glitr.annotation.GlitrDescription;
import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.util.ReflectionUtil;
import graphql.schema.*;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Factory implementation for the creation of {@link GraphQLObjectType}
 */
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
     * Creates the {@link GraphQLObjectType} dynamically for pretty much any pojo class with public accessors
     *
     * @param clazz class to be introspected
     * @return {@link GraphQLObjectType} object exposed via graphQL
     */
    private GraphQLObjectType createObjectType(Class clazz) {
        Map<String, Pair<Method, Class>> methods = ReflectionUtil.getMethodMap(clazz);

        // add extra methods from outside the inspected class coming from an override object
        typeRegistry.addExtraMethodsToTheSchema(clazz, methods);

        List<GraphQLFieldDefinition> fields = methods.values().stream()
                .map(pair -> getGraphQLFieldDefinition(clazz, pair))
                .collect(Collectors.toList());

        if (fields.size() == 0) {
            // GraphiQL doesn't like objects with no fields, so add an unused field to be safe.
            fields.add(newFieldDefinition()
                    .name("unused_fields_dead_object")
                    .type(GraphQLBoolean)
                    .staticValue(false)
                    .build());
        }

        // implemented interfaces
        List<GraphQLInterfaceType> graphQLInterfaceTypes = retrieveInterfacesForType(clazz);

        // extended abstract classes
        List<GraphQLInterfaceType> graphQLAbstractClassTypes = retrieveAbstractClassesForType(clazz);
        graphQLInterfaceTypes.addAll(graphQLAbstractClassTypes);

        GraphQLObjectType.Builder builder = newObject()
                .name(clazz.getSimpleName())
                .description(ReflectionUtil.getDescriptionFromAnnotatedElement(clazz))
                .withInterfaces(graphQLInterfaceTypes.toArray(new GraphQLInterfaceType[graphQLInterfaceTypes.size()]))
                .fields(fields);

        // relay is enabled, add Node interface implementation if one of the eligible methods is named getId
        if (typeRegistry.getNodeInterface() != null && methods.keySet().stream().anyMatch(name -> name.equals("getId"))
                && !typeRegistry.isExplicitRelayNodeScanEnabled()) {
            builder.withInterface(typeRegistry.getNodeInterface());
        }

        return builder.build();
    }

    /**
     * Look up and inspect abstract class extended by a specific class
     *
     * @param clazz being inspected
     * @return list of {@link GraphQLInterfaceType}
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
     * Look up and inspect interfaces implemented by a specific class
     *
     * @param clazz being inspected
     * @return list of {@link GraphQLInterfaceType}
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

    private GraphQLFieldDefinition getGraphQLFieldDefinition(Class clazz, Pair<Method, Class> pair) {
        // GraphQL Field Name
        Method method = pair.getLeft();
        String name = ReflectionUtil.sanitizeMethodName(method.getName());

        // DataFetcher
        Class declaringClass = pair.getRight();
        List<DataFetcher> fetchers = typeRegistry.retrieveDataFetchers(clazz, declaringClass, method);
        DataFetcher dataFetcher = TypeRegistry.createDataFetchersFromDataFetcherList(fetchers, declaringClass, name);

        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);
        if (description != null && description.equals(GlitrDescription.DEFAULT_DESCRIPTION)) {
            description = ReflectionUtil.getDescriptionFromAnnotatedField(clazz, method);
        }

        return newFieldDefinition()
                .name(name)
                .description(description)
                .dataFetcher(dataFetcher)
                .type(typeRegistry.retrieveGraphQLOutputType(declaringClass, method))
                .argument(typeRegistry.retrieveArguments(declaringClass, method))
                // TODO: static value
                // TODO: deprecation reason
                .build();
    }
}
