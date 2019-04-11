package com.nfl.glitr.registry.type;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.annotation.GlitrDeprecated;
import com.nfl.glitr.annotation.GlitrDescription;
import com.nfl.glitr.annotation.GlitrQueryComplexity;
import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.registry.schema.GlitrFieldDefinition;
import com.nfl.glitr.registry.schema.GlitrMetaDefinition;
import com.nfl.glitr.util.ReflectionUtil;
import graphql.language.InterfaceTypeDefinition;
import graphql.schema.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.nfl.glitr.util.NodeUtil.COMPLEXITY_FORMULA_KEY;
import static com.nfl.glitr.util.NodeUtil.COMPLEXITY_IGNORE_KEY;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * Factory implementation for the creation of {@link GraphQLInterfaceType}
 */
public class GraphQLInterfaceTypeFactory implements DelegateTypeFactory {

    private final TypeRegistry typeRegistry;
    private final GraphQLCodeRegistry.Builder codeRegistryBuilder;


    public GraphQLInterfaceTypeFactory(TypeRegistry typeRegistry, GraphQLCodeRegistry.Builder codeRegistryBuilder) {
        this.typeRegistry = typeRegistry;
        this.codeRegistryBuilder = codeRegistryBuilder;
    }

    @Override
    public GraphQLOutputType create(Class clazz) {
        return createInterfaceType(clazz);
    }

    /**
     * Creates the {@link GraphQLInterfaceType} dynamically for the given interface
     *
     * @param clazz class to be introspected
     * @return {@link GraphQLInterfaceType} object exposed via graphQL
     */
    private GraphQLInterfaceType createInterfaceType(Class clazz) {
        List<GraphQLFieldDefinition> fields = Arrays.stream(clazz.getMethods())
                .filter(ReflectionUtil::eligibleMethod)
                .sorted(Comparator.comparing(Method::getName))
                .map(method -> getGraphQLFieldDefinition(clazz, method))
                .collect(Collectors.toList());

        codeRegistryBuilder.typeResolver(clazz.getSimpleName(), typeRegistry);

        return newInterface()
                .name(clazz.getSimpleName())
                .definition(new InterfaceTypeDefinition(clazz.getCanonicalName()))
                .description(ReflectionUtil.getDescriptionFromAnnotatedElement(clazz))
                .fields(fields)
                .build();
    }

    private GraphQLFieldDefinition getGraphQLFieldDefinition(Class clazz, Method method) {
        String name = ReflectionUtil.sanitizeMethodName(method.getName());
        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);
        if (description != null && description.equals(GlitrDescription.DEFAULT_DESCRIPTION)) {
            description = ReflectionUtil.getDescriptionFromAnnotatedField(clazz, method);
        }

        Type fieldType = GenericTypeReflector.getExactReturnType(method, clazz);
        GraphQLType type = typeRegistry.convertToGraphQLOutputType(fieldType, name, true);
        if (type instanceof GraphQLTypeReference) {
            typeRegistry.getRegistry().putIfAbsent((Class) fieldType, type);
        }

        boolean nullable = ReflectionUtil.isAnnotatedElementNullable(method);
        if (nullable) { // check the field too
            try {
                Field field = clazz.getDeclaredField(name);
                nullable = ReflectionUtil.isAnnotatedElementNullable(field);
            } catch (NoSuchFieldException e) {
                // do nothing
            }
        }

        if (!nullable || name.equals("id")) {
            type = new GraphQLNonNull(type);
        }

        Optional<GlitrQueryComplexity> glitrQueryComplexity = ReflectionUtil.getAnnotationOfMethodOrField(clazz, method, GlitrQueryComplexity.class);
        Set<GlitrMetaDefinition> metaDefinitions = new HashSet<>();
        glitrQueryComplexity.ifPresent(queryComplexity -> {
            metaDefinitions.add(new GlitrMetaDefinition(COMPLEXITY_FORMULA_KEY, queryComplexity.value()));
            metaDefinitions.add(new GlitrMetaDefinition(COMPLEXITY_IGNORE_KEY, queryComplexity.ignore()));
        });

        Optional<GlitrDeprecated> glitrDeprecated = ReflectionUtil.getAnnotationOfMethodOrField(clazz, method, GlitrDeprecated.class);

        codeRegistryBuilder.dataFetcher(coordinates(clazz.getSimpleName(), name), new PropertyDataFetcher(name));

        return newFieldDefinition()
                .name(name)
                .description(description)
                .type((GraphQLOutputType) type)
                .definition(new GlitrFieldDefinition(name, metaDefinitions))
                .deprecate(glitrDeprecated.map(GlitrDeprecated::value).orElse(null))
                .build();
    }
}
