package com.nfl.dm.shield.graphql.domain.graph.annotation

@Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
annotation class GraphQLIgnore
