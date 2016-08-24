package com.nfl.dm.shield.graphql.registry.type;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.dm.shield.graphql.ReflectionUtil;
import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import graphql.schema.*;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;

public class GraphQLInputObjectTypeFactory implements DelegateTypeFactory {

    private final TypeRegistry typeRegistry;


    public GraphQLInputObjectTypeFactory(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    @Override
    public GraphQLType create(Class clazz) {
        return createInputObjectType(clazz);
    }

    public GraphQLInputObjectType createInputObjectType(Class clazz) {
        Map<String, Pair<Method, Class>> methods = ReflectionUtil.getMethodMap(clazz);

        List<GraphQLInputObjectField> fields = methods.values().stream()
                .map(this::getGraphQLInputObjectField)
                .collect(Collectors.toList());

        GraphQLInputObjectType.Builder builder = newInputObject()
                .name(clazz.getSimpleName())
                .description(ReflectionUtil.getDescriptionFromAnnotatedElement(clazz))
                .fields(fields);

        return builder.build();
    }

    private GraphQLInputObjectField getGraphQLInputObjectField(Pair<Method, Class> pair) {
        Method method = pair.getLeft();
        Class declaringClass = pair.getRight();

        String name = ReflectionUtil.sanitizeMethodName(method.getName());
        String description = ReflectionUtil.getDescriptionFromAnnotatedElement(method);
        GraphQLInputType graphQLInputType = (GraphQLInputType) typeRegistry.convertToGraphQLInputType(GenericTypeReflector.getExactReturnType(method, declaringClass), name);

        boolean nullable = ReflectionUtil.isAnnotatedElementNullable(method);
        if (!nullable || name.equals("id")) {
            graphQLInputType = new GraphQLNonNull(graphQLInputType);
        }

        return newInputObjectField()
                .type(graphQLInputType)
                .name(name)
                .description(description)
                // TODO: add default value feature, via annotation?
                .build();
    }
}
