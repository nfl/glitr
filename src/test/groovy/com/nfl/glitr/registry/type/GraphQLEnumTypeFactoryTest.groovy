package com.nfl.glitr.registry.type

import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLType
import spock.lang.Specification

class GraphQLEnumTypeFactoryTest extends Specification {

    def "Java enum type to GraphQLEnumType"() {
        setup:
        GraphQLEnumTypeFactory factory = new GraphQLEnumTypeFactory();
        when:
        GraphQLType graphQLType = factory.create(ProfileType)
        then:
        graphQLType instanceof GraphQLEnumType
        def profileType = (GraphQLEnumType) graphQLType
        profileType.name == ProfileType.simpleName
        profileType.values.value as Set == ProfileType.values() as Set
    }


    public enum ProfileType {
        PLAYER,
        COACH,
        CHEERLEADER
    }
}
