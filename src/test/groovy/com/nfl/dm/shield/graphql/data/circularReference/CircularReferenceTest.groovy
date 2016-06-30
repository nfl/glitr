package com.nfl.dm.shield.graphql.data.circularReference

import com.nfl.dm.shield.graphql.Glitr
import com.nfl.dm.shield.graphql.GlitrBuilder
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import spock.lang.Specification

class CircularReferenceTest extends Specification {


    def "Inspect AbstractClass that has a field circular reference"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().build()
        when:
        GraphQLType type = glitr.typeRegistry.lookup(AbstractRead.class)
        then: "Make sure an interface is created"
        type instanceof GraphQLInterfaceType
        type.name == AbstractRead.simpleName
        then: "And make sure the implementing type has the interface registered"
        def objType = glitr.typeRegistry.getType(new Novel())
        objType.class == GraphQLObjectType
        objType.name == Novel.simpleName
        objType.fieldDefinitions.name as Set == ["novel", "pageCount", "title", "reviewed"] as Set
        objType.interfaces.name as Set == [AbstractRead.simpleName] as Set
    }

    def "Inspect Interface that has a field circular reference"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().build()
        when:
        GraphQLType type = glitr.typeRegistry.lookup(Readable.class)
        then: "Make sure an interface is created"
        type instanceof GraphQLInterfaceType
        type.name == Readable.simpleName
        then: "And make sure the implementing type has the interface registered"
        def objType = glitr.typeRegistry.getType(new Book())
        objType.class == GraphQLObjectType
        objType.name == Book.simpleName
        objType.fieldDefinitions.name as Set == ["title", "synopsis"] as Set
        objType.interfaces.name as Set == [Readable.simpleName] as Set
    }
}
