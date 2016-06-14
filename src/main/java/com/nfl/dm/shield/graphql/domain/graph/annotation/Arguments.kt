package com.nfl.dm.shield.graphql.domain.graph.annotation

@Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Arguments(vararg val value: Argument)
