package com.nfl.glitr.graphql.registry

import com.nfl.glitr.graphql.Glitr
import com.nfl.glitr.graphql.GlitrBuilder
import com.nfl.glitr.graphql.data.mutation.Bitrate
import com.nfl.glitr.graphql.data.query.*
import com.nfl.glitr.graphql.registry.datafetcher.query.CompositeDataFetcher
import graphql.schema.*
import spock.lang.Specification

class TypeRegistryIntegrationTest extends Specification {

    def "Inspect queryType"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitrWithRelaySupport().withQueryRoot(new QueryType()).build()
        when:
        GraphQLObjectType type = (GraphQLObjectType) glitr.typeRegistry.lookup(QueryType.class)
        then:
        type.name == QueryType.class.getSimpleName()
        type.description == "No Description"
        // Relay Node field
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "node"
        fieldDef.description == "No Description"
        fieldDef.type instanceof GraphQLInterfaceType
        def GraphQLInterfaceType fieldDefType = (GraphQLInterfaceType) fieldDef.type
        fieldDefType.name == "Node"
        fieldDefType.description == "An object with an ID"
        fieldDefType.fieldDefinitions.size() == 1
        def input = (GraphQLArgument) fieldDef.arguments[0]
        input.name == "id"
        input.description == "No Description"
        input.type instanceof GraphQLNonNull
        def inputWrappedType = (GraphQLScalarType) ((GraphQLNonNull)input.type).wrappedType
        inputWrappedType.name == "ID"
        inputWrappedType.description == "Built-in ID"

        // Video field
        def fieldDef2 = type.fieldDefinitions[1]
        fieldDef2.name == "video"
        fieldDef2.description == "No Description"
        fieldDef2.type instanceof GraphQLObjectType
        def GraphQLObjectType fieldDefType2 = (GraphQLObjectType) fieldDef2.type
        fieldDefType2.name == "Video"
        fieldDefType2.description == "No Description"
        fieldDefType2.fieldDefinitions.size() == 5
        def input2 = (GraphQLArgument) fieldDef2.arguments[0]
        input2.name == "id"
        input2.description == "No Description"
        input2.type instanceof GraphQLNonNull
        def inputWrappedType2 = (GraphQLScalarType) ((GraphQLNonNull)input.type).wrappedType
        inputWrappedType2.name == "ID"
        inputWrappedType2.description == "Built-in ID"

        // Videos field
        def fieldDef3 = type.fieldDefinitions[2]
        fieldDef3.name == "videos"
        fieldDef3.description == "No Description"
        fieldDef3.arguments.name as Set == ["first", "after"] as Set
        fieldDef3.type instanceof GraphQLObjectType
        def GraphQLObjectType fieldDefType3 = (GraphQLObjectType) fieldDef3.type
        fieldDefType3.name == "VideoConnection"
        fieldDefType3.description == "A connection to a list of items."
        fieldDefType3.fieldDefinitions.size() == 2
        fieldDefType3.fieldDefinitions.name as Set == ["edges", "pageInfo"] as Set
    }

    def "Inspect Simple Object type"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLObjectType type = (GraphQLObjectType) glitr.typeRegistry.lookup(Bitrate.class)
        then:
        type.name == Bitrate.class.getSimpleName()
        type.description == "No Description"
        type.fieldDefinitions.name == ["createdDate", "durationNanos", "frames", "grade", "gradeAverage", "id", "kbps", "modifiedDateTime", "url", "valid"]
        type.interfaces.name as Set == ["Playable", "Node"] as Set
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "createdDate"
        fieldDef.description == "No Description"
        fieldDef.type instanceof GraphQLScalarType
        (fieldDef.type as GraphQLScalarType).name == "Date"
        def fieldDef2 = type.fieldDefinitions[1]
        fieldDef2.name == "durationNanos"
        fieldDef2.description == "No Description"
        fieldDef2.type instanceof GraphQLScalarType
        (fieldDef2.type as GraphQLScalarType).name == "Long"
        def fieldDef8 = type.fieldDefinitions[2]
        fieldDef8.name == "frames"
        fieldDef8.description == "No Description"
        fieldDef8.type instanceof GraphQLList
        def fieldDef3 = type.fieldDefinitions[3]
        fieldDef3.name == "grade"
        fieldDef3.description == "No Description"
        fieldDef3.type instanceof GraphQLScalarType
        (fieldDef3.type as GraphQLScalarType).name == "Float"
        def fieldDef4 = type.fieldDefinitions[4]
        fieldDef4.name == "gradeAverage"
        fieldDef4.description == "No Description"
        fieldDef4.type instanceof GraphQLScalarType
        (fieldDef4.type as GraphQLScalarType).name == "Float"
        def fieldDef5 = type.fieldDefinitions[5]
        fieldDef5.name == "id"
        fieldDef5.description == "No Description"
        fieldDef5.type instanceof GraphQLNonNull
        ((fieldDef5.type as GraphQLNonNull).wrappedType as GraphQLScalarType).name == "ID"
        def fieldDef6 = type.fieldDefinitions[6]
        fieldDef6.name == "kbps"
        fieldDef6.description == "No Description"
        fieldDef6.type instanceof GraphQLScalarType
        (fieldDef6.type as GraphQLScalarType).name == "Int"
        def fieldDef9 = type.fieldDefinitions[7]
        fieldDef9.name == "modifiedDateTime"
        fieldDef9.description == "No Description"
        fieldDef9.type instanceof GraphQLScalarType
        (fieldDef9.type as GraphQLScalarType).name == "DateTime"
        def fieldDef10 = type.fieldDefinitions[8]
        fieldDef10.name == "url"
        fieldDef10.description == "No Description"
        fieldDef10.type instanceof GraphQLScalarType
        (fieldDef10.type as GraphQLScalarType).name == "String"
        def fieldDef7 = type.fieldDefinitions[9]
        fieldDef7.name == "valid"
        fieldDef7.description == "No Description"
        fieldDef7.type instanceof GraphQLScalarType
        (fieldDef7.type as GraphQLScalarType).name == "Boolean"
    }

    def "Inspect Enum type"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLEnumType type = (GraphQLEnumType) glitr.typeRegistry.lookup(ProfileType.class)
        then:
        type.name == ProfileType.class.getSimpleName()
        type.description == "No Description"
        type.values.name as Set == [ProfileType.PLAYER.name(), ProfileType.COACH.name(), ProfileType.CHEERLEADER.name()] as Set
    }


    def "Inspect Interface type"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLInterfaceType type = (GraphQLInterfaceType) glitr.typeRegistry.lookup(Node.class)
        then:
        type.name == Node.class.getSimpleName()
        type.description == "Node interface needed for Relay"
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "id"
        fieldDef.description == "No Description"
        fieldDef.type instanceof GraphQLNonNull
        (fieldDef.type as GraphQLNonNull).wrappedType instanceof GraphQLScalarType
        def wrappedType = (GraphQLScalarType) (fieldDef.type as GraphQLNonNull).wrappedType
        wrappedType.name == "ID"
        fieldDef.dataFetcher instanceof CompositeDataFetcher
        (fieldDef.dataFetcher as CompositeDataFetcher).fetchers[0] instanceof PropertyDataFetcher
    }

    def "Inspect AbstractClass type"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLInterfaceType type = (GraphQLInterfaceType) glitr.typeRegistry.lookup(NodeAbstractClass.class)
        then:
        type.name == NodeAbstractClass.class.getSimpleName()
        type.description == "Node interface needed for Relay"
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "id"
        fieldDef.description == "No Description"
        fieldDef.type instanceof GraphQLNonNull
        (fieldDef.type as GraphQLNonNull).wrappedType instanceof GraphQLScalarType
        def wrappedType = (GraphQLScalarType) (fieldDef.type as GraphQLNonNull).wrappedType
        wrappedType.name == "ID"
        fieldDef.dataFetcher instanceof CompositeDataFetcher
        (fieldDef.dataFetcher as CompositeDataFetcher).fetchers[0] instanceof PropertyDataFetcher
    }

    def "Inspect Interface type that extends another interface"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLInterfaceType type = (GraphQLInterfaceType) glitr.typeRegistry.lookup(Playable.class)
        then: "Should inherit the fields from the super interfaces"
        type.name == Playable.simpleName
        type.fieldDefinitions.name == ["id", "url"]
    }


    def "Inspect Simple Object type that extends an abstract class and interfaces"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLObjectType type = (GraphQLObjectType) glitr.typeRegistry.lookup(Video.class)
        then: "Make sure it inherits the fields of the super classes"
        type.name == Video.class.getSimpleName()
        type.description == "No Description"
        type.fieldDefinitions.name == ["bitrateList", "id", "lastModifiedDate", "title", "url"]
        then: "Make sure it implements the interfaces"
        type.interfaces.name as Set == [AbstractContent.class.simpleName, AbstractTimestamped.class.simpleName,  Node.simpleName, Playable.class.simpleName] as Set
    }
}
