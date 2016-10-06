package com.nfl.glitr.registry.type;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.registry.TypeRegistry;
import com.nfl.glitr.util.ReflectionUtil;
import com.nfl.glitr.exception.GlitrException;
import graphql.schema.*;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;

/**
 * Factory implementation for the creation of {@link GraphQLInputObjectType}
 */
public class GraphQLInputObjectTypeFactory implements DelegateTypeFactory {

    private final TypeRegistry typeRegistry;


    public GraphQLInputObjectTypeFactory(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    @Override
    public GraphQLType create(Class clazz) {
        return createInputObjectType(clazz);
    }

    /**
     * Creates the {@link GraphQLInputObjectType} dynamically for the given class
     *
     * @param clazz class to be introspected
     * @return {@link GraphQLInputObjectType} object exposed via graphQL
     */
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

        GraphQLType graphQLType = typeRegistry.convertToGraphQLInputType(GenericTypeReflector.getExactReturnType(method, declaringClass), name);
        if (!(graphQLType instanceof GraphQLInputType)) {
            throw new GlitrException("Failed to create GraphQLInputType [" + graphQLType.getName() + "] in class [" + declaringClass.getName() + "]. " +
                    "This is most often the result of a GraphQLOutputType of the same name already existing as input types require unique names. " +
                    "Please make sure the name used for this input does not match that of another domain class.");
        }
        GraphQLInputType graphQLInputType = (GraphQLInputType) graphQLType;

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
