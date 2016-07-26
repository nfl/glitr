package com.nfl.dm.shield.graphql.registry.type

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
        profileType.values.value == ProfileType.values()
    }


    public enum ProfileType {
        PLAYER,
        COACH,
        CHEERLEADER
    }
}
