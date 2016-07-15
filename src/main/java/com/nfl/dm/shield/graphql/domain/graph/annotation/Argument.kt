package com.nfl.dm.shield.graphql.domain.graph.annotation

import kotlin.reflect.KClass

/**
 * name: name of the graphql argument
 * type: java type for the argument
 * argumentType: see ArgumentType.class
 * customQueryFragmentProvider: bean name of the custrom query provider.  Only applies when argumentType = ArgumentType.QUERY_CUSTOM
 * nullable: whether graphql argument is optional
 */
@Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Repeatable
annotation class Argument(
        val name: String,
        val type: KClass<*>,
        val nullable: Boolean = true,
        val defaultValue: String = "No Default Value",
        val description: String = "No Description")
