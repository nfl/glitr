package com.nfl.glitr.relay.type;

import com.nfl.glitr.registry.RelayNode;
import com.nfl.glitr.relay.RelayHelper;
import graphql.schema.GraphQLOutputType;
import rx.functions.Func4;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 *  Output type converter function for paging arguments annotations.
 */
public class RelayNodeOutputTypeFunc implements Func4<Field, Method, Class, Annotation, GraphQLOutputType> {

    private RelayHelper relayHelper;


    @Override
    public GraphQLOutputType call(@Nullable Field field, Method method, Class declaringClass, Annotation annotation) {
        if (annotation.annotationType() != RelayNode.class) {
            throw new IllegalArgumentException(annotation.annotationType() + " must be " + RelayNode.class.getSimpleName());
        }
        return relayHelper.getNodeInterface();
    }

    public RelayNodeOutputTypeFunc setRelayHelper(RelayHelper relayHelper) {
        this.relayHelper = relayHelper;
        return this;
    }
}
