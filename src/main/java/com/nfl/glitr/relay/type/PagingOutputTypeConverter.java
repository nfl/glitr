package com.nfl.glitr.relay.type;

import com.google.common.collect.Lists;
import com.googlecode.gentyref.GenericTypeReflector;
import com.nfl.glitr.exception.GlitrException;
import com.nfl.glitr.relay.RelayHelper;
import com.nfl.glitr.util.ReflectionUtil;
import com.nfl.glitr.annotation.GlitrForwardPagingArguments;
import com.nfl.glitr.annotation.GlitrNonNull;
import com.nfl.glitr.registry.TypeRegistry;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import rx.functions.Func4;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

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
        Class endEdgeClass =  ReflectionUtil.getClassFromType(endEdgeType);

        // find that class from the registry (or lookup if first time)
        GraphQLOutputType edgeGraphQLOutputType = (GraphQLOutputType) typeRegistry.convertToGraphQLOutputType(endEdgeType, endEdgeClass.getSimpleName());

        // is this an optional field
        boolean nullable = !method.isAnnotationPresent(GlitrNonNull.class)
                && (field == null || !field.isAnnotationPresent(GlitrNonNull.class));

        if (!nullable) {
            edgeGraphQLOutputType = new GraphQLNonNull(edgeGraphQLOutputType);
        }

        // build a relay edge
        GraphQLObjectType edgeType = relayHelper.edgeType(endEdgeClass.getSimpleName(),
                                                    edgeGraphQLOutputType,
                                                    relayHelper.getNodeInterface(),
                                                    Collections.emptyList());
        // build the relay connection
        GraphQLObjectType connectionType = relayHelper.connectionType(endEdgeClass.getSimpleName(), edgeType, Lists.newArrayList());

        // check if a connection with this name already exists
        Map<String, GraphQLType> nameRegistry = typeRegistry.getNameRegistry();
        GraphQLObjectType qlObjectType = (GraphQLObjectType) nameRegistry.get(connectionType.getName());
        if (qlObjectType != null) {
            // TODO: better equality function
            if (!qlObjectType.toString().equals(connectionType.toString())) {
                throw new GlitrException("Attempting to create two types with the same name. All types within a GraphQL schema must have unique names. " +
                        "You have defined the type [" + connectionType.getName() + "] as both [" + qlObjectType + "] and [" + connectionType + "]");
            }
            return qlObjectType;
        }

        // add the connection to the registry and return the connection
        nameRegistry.put(connectionType.getName(), connectionType);
        return connectionType;
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
