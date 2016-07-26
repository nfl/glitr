package com.nfl.dm.shield.graphql.registry.type;

import com.nfl.dm.shield.graphql.ReflectionUtil;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLType;

import static graphql.schema.GraphQLEnumType.newEnum;

public class GraphQLEnumTypeFactory implements DelegateTypeFactory {

    @Override
    public GraphQLType create(Class clazz) {
        return createEnumType(clazz);
    }

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
