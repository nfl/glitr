package com.nfl.dm.shield.graphql.domain.graph.annotation

/**
 * Designate a GraphQL result or input field that can't be `null`
 */
@Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
annotation class GraphQLNonNull
