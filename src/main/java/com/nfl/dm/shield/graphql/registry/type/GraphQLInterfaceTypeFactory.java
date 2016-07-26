package com.nfl.dm.shield.graphql.registry.type;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.dm.shield.graphql.ReflectionUtil;
import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import com.nfl.dm.shield.graphql.registry.datafetcher.query.batched.CompositeDataFetcherFactory;
import graphql.schema.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;

public class GraphQLInterfaceTypeFactory implements DelegateTypeFactory {

    private final TypeRegistry typeRegistry;

    public GraphQLInterfaceTypeFactory(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    @Override
    public GraphQLOutputType create(Class clazz) {
        return createInterfaceType(clazz);
    }

    private GraphQLInterfaceType createInterfaceType(Class clazz) {
        List<GraphQLFieldDefinition> fields = Arrays.stream(clazz.getMethods())
                .filter(ReflectionUtil::eligibleMethod)
                .sorted(Comparator.comparing(Method::getName))
                .map(method -> {
                    String name = ReflectionUtil.sanitizeMethodName(method.getName());

                    // description
                    String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);

                    // type
                    Type fieldType = GenericTypeReflector.getExactReturnType(method, clazz);
                    GraphQLType type = typeRegistry.convertToGraphQLOutputType(fieldType, name, true);

                    if (type instanceof GraphQLTypeReference) {
                        typeRegistry.getRegistry().putIfAbsent((Class) fieldType, type);
                    }

                    // nullable
                    boolean nullable = ReflectionUtil.isAnnotatedElementNullable(method);

                    if (!nullable || name.equals("id")) {
                        type = new GraphQLNonNull(type);
                    }

                    return newFieldDefinition()
                            .name(name)
                            .description(description)
                            .dataFetcher(CompositeDataFetcherFactory.create(Collections.singletonList(new PropertyDataFetcher(name))))
                            .type((GraphQLOutputType) type)
                            .build();
                })
                .collect(Collectors.toList());

        // description
        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(clazz);

        return newInterface()
                .name(clazz.getSimpleName())
                .description(description)
                .typeResolver(typeRegistry)
                .fields(fields)
                .build();
    }
}
