package com.nfl.glitr.relay.type;

import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.annotation.GlitrForwardPagingArguments;
import graphql.schema.GraphQLArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func4;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;

/**
 * Create CustomFieldArgumentsFunc based on annotation.
 */
public class CustomFieldArgumentsFunc implements Func4<Field, Method, Class, Annotation, List<GraphQLArgument>> {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(CustomFieldArgumentsFunc.class);


    @Override
    public List<GraphQLArgument> call(@Nullable Field field, Method method, Class declaringClass, Annotation annotation) {
        // if same annotation is detected on both field and getter we fail. Only one annotation is allowed. We can talk about having precedence logic later.
        if (method.isAnnotationPresent(annotation.annotationType()) && field != null && field.isAnnotationPresent(annotation.annotationType())) {
            throw new IllegalArgumentException("Conflict: GraphQL Annotations can't be added to both field and getter. Pick one for "+
                                                       annotation.annotationType() + " on " + field.getName() + " and " + method.getName());
        }

        Type returnType = GenericTypeReflector.getExactReturnType(method, declaringClass);
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;

            Type containerType = parameterizedType.getRawType();
            if (Collection.class.isAssignableFrom((Class) containerType)) {
                List<GraphQLArgument> arguments = new ArrayList<>();
                arguments.add(newArgument().name(GlitrForwardPagingArguments.FIRST).type(GraphQLInt).build());
                arguments.add(newArgument().name(GlitrForwardPagingArguments.AFTER).type(GraphQLString).build());
                return arguments;
            }
        }

        return new ArrayList<>();
    }
}
