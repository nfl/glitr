package com.nfl.glitr.registry.type

import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLType
import spock.lang.Specification

import static com.nfl.glitr.util.NamingUtil.compatibleClassName

class GraphQLEnumTypeFactoryTest extends Specification {

    def "Java enum type to GraphQLEnumType"() {
        setup:
            GraphQLEnumTypeFactory factory = new GraphQLEnumTypeFactory()
        when:
            GraphQLType graphQLType = factory.create(ProfileType)
        then:
            graphQLType instanceof GraphQLEnumType
            def profileType = (GraphQLEnumType) graphQLType
            profileType.name == compatibleClassName(ProfileType)
            profileType.values.value == ProfileType.values()
    }


    enum ProfileType {
        PLAYER,
        COACH,
        CHEERLEADER
    }
}
