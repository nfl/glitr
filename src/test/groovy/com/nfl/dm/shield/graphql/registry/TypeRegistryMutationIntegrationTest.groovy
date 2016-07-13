package com.nfl.dm.shield.graphql.registry

import com.nfl.dm.shield.graphql.Glitr
import com.nfl.dm.shield.graphql.GlitrBuilder
import com.nfl.dm.shield.graphql.data.mutation.MutationType
import com.nfl.dm.shield.graphql.data.mutation.VideoMutationIn
import com.nfl.dm.shield.graphql.data.mutation.VideoMutationPayload
import com.nfl.dm.shield.graphql.data.query.QueryType
import graphql.GraphQL
import graphql.schema.*
import org.apache.commons.lang3.StringUtils
import spock.lang.Specification

class TypeRegistryMutationIntegrationTest extends Specification {

    def "Inspect mutationType generated schema"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitr()
                .withQueryRoot(new QueryType()).build()
        when:
        GraphQLObjectType mutationType = (GraphQLObjectType) glitr.typeRegistry.createRelayMutationType(MutationType.class)

        then: "Check the mutation field"
        mutationType.name == MutationType.class.getSimpleName()
        mutationType.description == "No Description"
        def fieldDef = mutationType.fieldDefinitions[0]
        fieldDef.name == "saveVideoInfoMutation"
        fieldDef.description == "Saves Info related to a video"
        fieldDef.type instanceof GraphQLObjectType

        then: "Check that output is correctly constructed"
        def GraphQLObjectType fieldDefType = (GraphQLObjectType) fieldDef.type
        fieldDefType.name == VideoMutationPayload.class.simpleName
        fieldDefType.description == "No Description"
        fieldDefType.fieldDefinitions.size() == 2
        fieldDefType.fieldDefinitions.name as Set == ["clientMutationId", StringUtils.uncapitalize(VideoMutationPayload.class.simpleName)] as Set
        def GraphQLObjectType fieldDefOutputType = (GraphQLObjectType) fieldDefType.fieldDefinitions[1].type
        fieldDefOutputType.fieldDefinitions.size() == 4
        fieldDefOutputType.fieldDefinitions.name as Set == ["url", "title", "bitrateList", "id"] as Set

        then: "Check that then input is correctly constructed"
        def input = (GraphQLArgument) fieldDef.arguments[0]
        input.name == "input"
        input.description == "No Description"
        input.defaultValue == "{default input}"
        input.type instanceof GraphQLNonNull
        def inputWrappedType = (GraphQLInputObjectType) ((GraphQLNonNull)input.type).wrappedType
        inputWrappedType.name == "VideoMutationInput"
        inputWrappedType.description == "Relay mutation input"
        inputWrappedType.fields.each {it -> it instanceof GraphQLInputObjectField }
        inputWrappedType.fields.name as Set == ["clientMutationId", "videoMutation"] as Set
        def GraphQLInputObjectField fieldDefInputType = (GraphQLInputObjectField) inputWrappedType.fields[1]
        fieldDefInputType.description == "Info meta data needed"
        fieldDefInputType.defaultValue == null
        fieldDefInputType.type instanceof GraphQLNonNull
        def inputFieldWrappedType = (GraphQLInputObjectType) ((GraphQLNonNull)fieldDefInputType.type).wrappedType
        inputFieldWrappedType.name == VideoMutationIn.class.simpleName
        inputFieldWrappedType.description == "No Description"
        inputFieldWrappedType.fields.name as Set == ["url", "title", "bitrateList"] as Set
        inputFieldWrappedType.fields[1].type instanceof GraphQLScalarType
        inputFieldWrappedType.fields[2].type instanceof GraphQLScalarType
        inputFieldWrappedType.fields[0].type instanceof GraphQLList
        inputFieldWrappedType.fields.defaultValue == [null, null, null]
    }

    def "Perform a mutation against mutationType"() {
        setup:
        Glitr glitr = GlitrBuilder.newGlitrWithRelaySupport()
                .withQueryRoot(new QueryType())
                .withMutationRoot(new MutationType())
                .build()

        GraphQL graphQL =  new GraphQL(glitr.getSchema());
        when:
        def data = graphQL.execute("""
            mutation {
                saveVideoInfoMutation(input: {
                    clientMutationId: \"mutationId-Sx160620160639713-1\"
                    videoMutation: {
                        title: "My video title"
                    }
                }){
                    clientMutationId
                    videoMutationPayload {
                        title
                    }
                }
            }
        """).data;
        then:
        (data as Map).saveVideoInfoMutation?.videoMutationPayload?.title == "My video title"
        (data as Map).saveVideoInfoMutation?.clientMutationId == "mutationId-Sx160620160639713-1"
    }
}
