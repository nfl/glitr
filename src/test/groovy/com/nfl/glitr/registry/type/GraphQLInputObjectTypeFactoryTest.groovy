package com.nfl.glitr.registry.type

import com.nfl.glitr.exception.GlitrException
import com.nfl.glitr.registry.TypeRegistry
import com.nfl.glitr.registry.TypeRegistryBuilder
import com.nfl.glitr.util.NamingUtil
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLType
import spock.lang.Specification

public class GraphQLInputObjectTypeFactoryTest extends Specification {

    def "Java class to GraphQLInputType"() {
        setup:
        TypeRegistry typeRegistry = TypeRegistryBuilder.newTypeRegistry().build()
        GraphQLInputObjectTypeFactory factory = new GraphQLInputObjectTypeFactory(typeRegistry)
        when:
        GraphQLType graphQLType = factory.create(Test)
        then:
        graphQLType instanceof GraphQLInputType
        def video = (GraphQLInputType) graphQLType
        video.name == NamingUtil.compatibleClassName(Test)
    }

    def "Java class to GraphQLInputType with name already in use"() {
        setup:
        TypeRegistry typeRegistry = TypeRegistryBuilder.newTypeRegistry().build()
        GraphQLObjectTypeFactory objFactory = new GraphQLObjectTypeFactory(typeRegistry)
        GraphQLInputObjectTypeFactory inputFactory = new GraphQLInputObjectTypeFactory(typeRegistry)

        when:
        objFactory.create(Test)
        inputFactory.create(TestInput)

        then:
        thrown GlitrException
    }
}
