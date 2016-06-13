package com.nfl.dm.shield.graphql.relay.type;

import com.google.common.collect.Lists;
import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.dm.shield.graphql.NodeUtil;
import com.nfl.dm.shield.graphql.domain.graph.annotation.ForwardPagingArguments;
import com.nfl.dm.shield.graphql.registry.TypeRegistry;
import com.nfl.dm.shield.graphql.relay.RelayHelper;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import rx.functions.Func4;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;

import static com.nfl.dm.shield.graphql.registry.TypeRegistry.getActualTypeArgumentFromType;

/**
 *  Output type converter function for paging arguments annotations.
 */
public class PagingOutputTypeConverter implements Func4<Field, Method, Class, Annotation, GraphQLOutputType> {

    private final RelayHelper relayHelper;
    private final TypeRegistry typeRegistry;

    public PagingOutputTypeConverter(RelayHelper relayHelper, TypeRegistry typeRegistry) {
        this.relayHelper = relayHelper;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public GraphQLOutputType call(@Nullable Field field, Method method, Class declaringClass, Annotation annotation) {

        if (annotation.annotationType() != ForwardPagingArguments.class) {
            throw new IllegalArgumentException(annotation.annotationType() + " must be " + ForwardPagingArguments.class.getSimpleName());
        }

        Type returnType = GenericTypeReflector.getExactReturnType(method, declaringClass);

        // Paging requires we actually get a collection of something back
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(returnType + " has to be of type ParametrizedType");
        }

        ParameterizedType parameterizedType = (ParameterizedType) returnType;

        // e.g: For List<Thing>, we attempt to extract class `Thing`
        Type endEdgeType = getActualTypeArgumentFromType(parameterizedType);
        Class endEdgeClass =  NodeUtil.getClassFromType(endEdgeType);

        // Find that class from the registry (or lookup if first time)
        GraphQLOutputType edgeGraphQLOutputType = (GraphQLOutputType) typeRegistry.convertToGraphQLOutputType(endEdgeType, endEdgeClass.getSimpleName());

        // nullable
        boolean nullable = !method.isAnnotationPresent(com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull.class)
                && (field != null && !field.isAnnotationPresent(com.nfl.dm.shield.graphql.domain.graph.annotation.GraphQLNonNull.class));

        if (!nullable) {
            edgeGraphQLOutputType = new GraphQLNonNull(edgeGraphQLOutputType);
        }

        // Build a relay edge
        GraphQLObjectType edgeType = RelayHelper.edgeType(endEdgeClass.getSimpleName(),
                                                    edgeGraphQLOutputType,
                                                    relayHelper.getNodeInterface(),
                                                    Collections.emptyList());
        // Last build the relay connection!
        return RelayHelper.connectionType(endEdgeClass.getSimpleName(), edgeType, Lists.newArrayList());
    }
}
