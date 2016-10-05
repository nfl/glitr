package com.nfl.glitr.graphql.relay.type;

import com.google.common.collect.Lists;
import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.graphql.NodeUtil;
import com.nfl.glitr.graphql.ReflectionUtil;
import com.nfl.glitr.graphql.domain.graph.annotation.GlitrForwardPagingArguments;
import com.nfl.glitr.graphql.domain.graph.annotation.GlitrNonNull;
import com.nfl.glitr.graphql.registry.TypeRegistry;
import com.nfl.glitr.graphql.relay.RelayHelper;
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

/**
 *  Output type converter function for paging arguments annotations.
 */
public class PagingOutputTypeConverter implements Func4<Field, Method, Class, Annotation, GraphQLOutputType> {

    private RelayHelper relayHelper;
    private TypeRegistry typeRegistry;


    @Override
    public GraphQLOutputType call(@Nullable Field field, Method method, Class declaringClass, Annotation annotation) {
        if (annotation.annotationType() != GlitrForwardPagingArguments.class) {
            throw new IllegalArgumentException(annotation.annotationType() + " must be " + GlitrForwardPagingArguments.class.getSimpleName());
        }

        Type returnType = GenericTypeReflector.getExactReturnType(method, declaringClass);

        // paging requires we actually get a collection of something back
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(returnType + " has to be of type ParametrizedType");
        }

        ParameterizedType parameterizedType = (ParameterizedType) returnType;

        // e.g: for List<Thing>, we attempt to extract class `Thing`
        Type endEdgeType = ReflectionUtil.getActualTypeArgumentFromType(parameterizedType);
        Class endEdgeClass =  NodeUtil.getClassFromType(endEdgeType);

        // find that class from the registry (or lookup if first time)
        GraphQLOutputType edgeGraphQLOutputType = (GraphQLOutputType) typeRegistry.convertToGraphQLOutputType(endEdgeType, endEdgeClass.getSimpleName());

        // is this an optional field
        boolean nullable = !method.isAnnotationPresent(GlitrNonNull.class)
                && (field != null && !field.isAnnotationPresent(GlitrNonNull.class));

        if (!nullable) {
            edgeGraphQLOutputType = new GraphQLNonNull(edgeGraphQLOutputType);
        }

        // build a relay edge
        GraphQLObjectType edgeType = relayHelper.edgeType(endEdgeClass.getSimpleName(),
                                                    edgeGraphQLOutputType,
                                                    relayHelper.getNodeInterface(),
                                                    Collections.emptyList());
        // last build the relay connection!
        return relayHelper.connectionType(endEdgeClass.getSimpleName(), edgeType, Lists.newArrayList());
    }

    public PagingOutputTypeConverter setTypeRegistry(TypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
        return this;
    }

    public PagingOutputTypeConverter setRelayHelper(RelayHelper relayHelper) {
        this.relayHelper = relayHelper;
        return this;
    }
}
