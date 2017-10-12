package com.nfl.glitr.registry

import com.nfl.glitr.Glitr
import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.annotation.GlitrArgument
import com.nfl.glitr.annotation.GlitrDescription
import com.nfl.glitr.data.mutation.Bitrate
import com.nfl.glitr.data.query.*
import com.nfl.glitr.registry.datafetcher.query.CompositeDataFetcher
import com.nfl.glitr.util.SerializationUtil
import graphql.schema.*
import spock.lang.Specification

class TypeRegistryIntegrationTest extends Specification {

    def "Inspect queryType"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr()
                .withRelay()
                .withObjectMapper(SerializationUtil.objectMapper)
                .withQueryRoot(new QueryType())
                .build()
        when:
        GraphQLObjectType type = (GraphQLObjectType) glitr.typeRegistry.lookup(QueryType.class)
        then:
        type.name == QueryType.class.getSimpleName()
        type.description == GlitrDescription.DEFAULT_DESCRIPTION

        // Relay Identifiable field
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "node"
        fieldDef.description == null
        fieldDef.type instanceof GraphQLInterfaceType
        GraphQLInterfaceType fieldDefType = (GraphQLInterfaceType) fieldDef.type
        fieldDefType.name == "Node"
        fieldDefType.description == "An object with an ID"
        fieldDefType.fieldDefinitions.size() == 1
        def input = (GraphQLArgument) fieldDef.arguments[0]
        input.name == "id"
        input.description == GlitrArgument.DEFAULT_DESCRIPTION
        input.type instanceof GraphQLNonNull
        def inputWrappedType = (GraphQLScalarType) ((GraphQLNonNull)input.type).wrappedType
        inputWrappedType.name == "ID"
        inputWrappedType.description == "Built-in ID"

        // Nodes field
        def fieldDef1 = type.fieldDefinitions[1]
        fieldDef1.name == "nodes"
        fieldDef1.description == null
        fieldDef1.type instanceof GraphQLList
        GraphQLList fieldDefType1 = (GraphQLList) fieldDef1.type
        GraphQLInterfaceType wrappedType1 = (GraphQLInterfaceType) fieldDefType1.wrappedType
        wrappedType1.name == "Node"
        wrappedType1.description == "An object with an ID"
        wrappedType1.fieldDefinitions.size() == 1
        def input1 = (GraphQLArgument) fieldDef1.arguments[0]
        input1.name == "ids"
        input1.description == GlitrArgument.DEFAULT_DESCRIPTION
        input1.type instanceof GraphQLNonNull
        def inputWrappedType1 = (GraphQLScalarType) ((GraphQLNonNull)input.type).wrappedType
        inputWrappedType1.name == "ID"
        inputWrappedType1.description == "Built-in ID"

        // Other Video field
        def fieldDef2 = type.fieldDefinitions[2]
        fieldDef2.name == "otherVideos"
        fieldDef2.description == null
        fieldDef2.arguments.name as Set == ["first", "after"] as Set
        fieldDef2.type instanceof GraphQLObjectType
        GraphQLObjectType fieldDefType2 = (GraphQLObjectType) fieldDef2.type
        fieldDefType2.name == "VideoConnection"
        fieldDefType2.description == "A connection to a list of items."
        fieldDefType2.fieldDefinitions.size() == 2
        fieldDefType2.fieldDefinitions.name as Set == ["edges", "pageInfo"] as Set

        // Video field
        def fieldDef3 = type.fieldDefinitions[3]
        fieldDef3.name == "video"
        fieldDef3.description == null
        fieldDef3.type instanceof GraphQLObjectType
        GraphQLObjectType fieldDefType3 = (GraphQLObjectType) fieldDef3.type
        fieldDefType3.name == "Video"
        fieldDefType3.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDefType3.fieldDefinitions.size() == 5
        def input3 = (GraphQLArgument) fieldDef3.arguments[0]
        input3.name == "id"
        input3.description == GlitrArgument.DEFAULT_DESCRIPTION
        input3.type instanceof GraphQLNonNull
        def inputWrappedType3 = (GraphQLScalarType) ((GraphQLNonNull)input3.type).wrappedType
        inputWrappedType3.name == "ID"
        inputWrappedType3.description == "Built-in ID"

        // Videos field
        def fieldDef4 = type.fieldDefinitions[4]
        fieldDef4.name == "videos"
        fieldDef4.description == null
        fieldDef4.arguments.name as Set == ["first", "after"] as Set
        fieldDef4.type instanceof GraphQLObjectType
        GraphQLObjectType fieldDefType4 = (GraphQLObjectType) fieldDef4.type
        fieldDefType4.name == "VideoConnection"
        fieldDefType4.description == "A connection to a list of items."
        fieldDefType4.fieldDefinitions.size() == 2
        fieldDefType4.fieldDefinitions.name as Set == ["edges", "pageInfo"] as Set

        // ZZZ Nodes field
        def fieldDef5 = type.fieldDefinitions[5]
        fieldDef5.name == "zZZNodes"
        fieldDef5.description == null
        fieldDef5.type instanceof GraphQLList
        GraphQLList fieldDefType5 = (GraphQLList) fieldDef5.type
        GraphQLInterfaceType wrappedType5 = (GraphQLInterfaceType) fieldDefType5.wrappedType
        wrappedType5.name == "Node"
        wrappedType5.description == "An object with an ID"
        wrappedType5.fieldDefinitions.size() == 1
        def input5 = (GraphQLArgument) fieldDef5.arguments[0]
        input5.name == "ids"
        input5.description == GlitrArgument.DEFAULT_DESCRIPTION
        input5.type instanceof GraphQLNonNull
        def inputWrappedType5 = (GraphQLScalarType) ((GraphQLNonNull)input.type).wrappedType
        inputWrappedType5.name == "ID"
        inputWrappedType5.description == "Built-in ID"

        // ZZZ Videos field
        def fieldDef6 = type.fieldDefinitions[6]
        fieldDef6.name == "zZZVideos"
        fieldDef6.description == null
        fieldDef6.type instanceof GraphQLList
        GraphQLList fieldDefType6 = (GraphQLList) fieldDef6.type
        GraphQLObjectType wrappedType6 = (GraphQLObjectType) fieldDefType6.wrappedType
        wrappedType6.name == "Video"
        wrappedType6.description == GlitrDescription.DEFAULT_DESCRIPTION
        wrappedType6.fieldDefinitions.size() == 5
    }

    def "Inspect Simple Object type"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLObjectType type = (GraphQLObjectType) glitr.typeRegistry.lookup(Bitrate.class)
        then:
        type.name == Bitrate.class.getSimpleName()
        type.description == GlitrDescription.DEFAULT_DESCRIPTION
        type.fieldDefinitions.name == ["createdDate", "durationNanos", "frames", "grade", "gradeAverage", "id", "kbps", "modifiedDateTime", "url", "valid"]
        type.interfaces.name as Set == ["Playable", "Identifiable"] as Set
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "createdDate"
        fieldDef.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef.type instanceof GraphQLScalarType
        (fieldDef.type as GraphQLScalarType).name == "Date"
        def fieldDef2 = type.fieldDefinitions[1]
        fieldDef2.name == "durationNanos"
        fieldDef2.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef2.type instanceof GraphQLScalarType
        (fieldDef2.type as GraphQLScalarType).name == "Long"
        def fieldDef8 = type.fieldDefinitions[2]
        fieldDef8.name == "frames"
        fieldDef8.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef8.type instanceof GraphQLList
        def fieldDef3 = type.fieldDefinitions[3]
        fieldDef3.name == "grade"
        fieldDef3.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef3.type instanceof GraphQLScalarType
        (fieldDef3.type as GraphQLScalarType).name == "Float"
        def fieldDef4 = type.fieldDefinitions[4]
        fieldDef4.name == "gradeAverage"
        fieldDef4.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef4.type instanceof GraphQLScalarType
        (fieldDef4.type as GraphQLScalarType).name == "Float"
        def fieldDef5 = type.fieldDefinitions[5]
        fieldDef5.name == "id"
        fieldDef5.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef5.type instanceof GraphQLNonNull
        ((fieldDef5.type as GraphQLNonNull).wrappedType as GraphQLScalarType).name == "ID"
        def fieldDef6 = type.fieldDefinitions[6]
        fieldDef6.name == "kbps"
        fieldDef6.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef6.type instanceof GraphQLScalarType
        (fieldDef6.type as GraphQLScalarType).name == "Int"
        def fieldDef9 = type.fieldDefinitions[7]
        fieldDef9.name == "modifiedDateTime"
        fieldDef9.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef9.type instanceof GraphQLScalarType
        (fieldDef9.type as GraphQLScalarType).name == "DateTime"
        def fieldDef10 = type.fieldDefinitions[8]
        fieldDef10.name == "url"
        fieldDef10.description == GlitrDescription.DEFAULT_DESCRIPTION
        fieldDef10.type instanceof GraphQLScalarType
        (fieldDef10.type as GraphQLScalarType).name == "String"
        def fieldDef7 = type.fieldDefinitions[9]
        fieldDef7.name == "valid"
        fieldDef7.description == GlitrDescription.DEFAULT_DESCRIPTION
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
        type.description == GlitrDescription.DEFAULT_DESCRIPTION
        type.values.name as Set == [ProfileType.PLAYER.name(), ProfileType.COACH.name(), ProfileType.CHEERLEADER.name()] as Set
    }


    def "Inspect Interface type"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
        GraphQLInterfaceType type = (GraphQLInterfaceType) glitr.typeRegistry.lookup(Identifiable.class)
        then:
        type.name == Identifiable.class.getSimpleName()
        type.description == "Identifiable interface needed for Relay"
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "id"
        fieldDef.description == GlitrDescription.DEFAULT_DESCRIPTION
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
        GraphQLInterfaceType type = (GraphQLInterfaceType) glitr.typeRegistry.lookup(IdentifiableAbstractClass.class)
        then:
        type.name == IdentifiableAbstractClass.class.getSimpleName()
        type.description == "Identifiable interface needed for Relay"
        def fieldDef = type.fieldDefinitions[0]
        fieldDef.name == "id"
        fieldDef.description == GlitrDescription.DEFAULT_DESCRIPTION
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
        type.description == GlitrDescription.DEFAULT_DESCRIPTION
        type.fieldDefinitions.name == ["bitrateList", "id", "lastModifiedDate", "title", "url"]
        then: "Make sure it implements the interfaces"
        type.interfaces.name as Set == [AbstractContent.class.simpleName, AbstractTimestamped.class.simpleName, Identifiable.simpleName, Playable.class.simpleName] as Set
    }

    def "Inspect Query Complexity Exclude Nodes"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryRoot(new QueryType())
                    .build()
        when:
            (GraphQLObjectType) glitr.typeRegistry.lookup(QueryType.class)
        then:
            def excludeNodes = glitr.typeRegistry.getQueryComplexityExcludeNodes()
            excludeNodes.contains("otherVideos->node")
            excludeNodes.contains("otherVideos->edges")
            excludeNodes.contains("videos->edges")
            excludeNodes.contains("videos->node")

            !excludeNodes.contains("zZZVideos->node")
            !excludeNodes.contains("zZZVideos->edges")
    }
}
