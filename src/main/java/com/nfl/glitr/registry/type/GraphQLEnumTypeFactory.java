package com.nfl.glitr.registry.type;

import com.nfl.glitr.util.ReflectionUtil;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLType;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * Factory implementation for the creation of {@link GraphQLEnumType}
 */
public class GraphQLEnumTypeFactory implements DelegateTypeFactory {

    @Override
    public GraphQLType create(Class clazz) {
        return createEnumType(clazz);
    }

    /**
     * Creates the {@link GraphQLEnumType} dynamically for the given enum
     *
     * @param clazz enum class to be introspected
     * @return {@link GraphQLEnumType} object exposed via graphQL
     */
    public static GraphQLEnumType createEnumType(Class clazz) {
        GraphQLEnumType.Builder builder = newEnum()
                .name(clazz.getSimpleName())
                .description(ReflectionUtil.getDescriptionFromAnnotatedElement(clazz));

        for (Object constant : clazz.getEnumConstants()) {
            builder.value(constant.toString(), constant);
        }

        return builder.build();
    }
}
