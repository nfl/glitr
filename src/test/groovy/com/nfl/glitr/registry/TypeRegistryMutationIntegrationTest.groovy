package com.nfl.glitr.registry

import com.nfl.glitr.Glitr
import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.annotation.GlitrArgument
import com.nfl.glitr.annotation.GlitrDescription
import com.nfl.glitr.data.mutation.MutationType
import com.nfl.glitr.data.mutation.VideoMutationIn
import com.nfl.glitr.data.mutation.VideoMutationPayload
import com.nfl.glitr.data.query.QueryType
import com.nfl.glitr.util.SerializationUtil
import graphql.GraphQL
import graphql.schema.*
import org.apache.commons.lang3.StringUtils
import spock.lang.Specification

import static com.nfl.glitr.util.NamingUtil.compatibleClassName

class TypeRegistryMutationIntegrationTest extends Specification {

    def "Inspect mutationType generated schema"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withQueryRoot(new QueryType()).build()
        when:
            GraphQLObjectType mutationType = (GraphQLObjectType) glitr.typeRegistry.createRelayMutationType(MutationType.class)

        then: "Check the mutation field"
            mutationType.name == compatibleClassName(MutationType)
            mutationType.description == GlitrDescription.DEFAULT_DESCRIPTION
            def fieldDef = mutationType.fieldDefinitions[0]
            fieldDef.name == "saveVideoInfoMutation"
            fieldDef.description == "Saves Info related to a video"
            fieldDef.type instanceof GraphQLObjectType

        then: "Check that output is correctly constructed"
            def GraphQLObjectType fieldDefType = (GraphQLObjectType) fieldDef.type
            fieldDefType.name == compatibleClassName(VideoMutationPayload)
            fieldDefType.description == GlitrDescription.DEFAULT_DESCRIPTION
            fieldDefType.fieldDefinitions.size() == 2
            fieldDefType.fieldDefinitions.name as Set == ["clientMutationId", StringUtils.uncapitalize(VideoMutationPayload.simpleName)] as Set
            def GraphQLObjectType fieldDefOutputType = (GraphQLObjectType) fieldDefType.fieldDefinitions[1].type
            fieldDefOutputType.fieldDefinitions.size() == 4
            fieldDefOutputType.fieldDefinitions.name as Set == ["url", "title", "bitrateList", "id"] as Set

        then: "Check that then input is correctly constructed"
            def input = (GraphQLArgument) fieldDef.arguments[0]
            input.name == "input"
            input.description == GlitrArgument.DEFAULT_DESCRIPTION
            input.defaultValue == "{default input}"
            input.type instanceof GraphQLNonNull
            def inputWrappedType = (GraphQLInputObjectType) ((GraphQLNonNull) input.type).wrappedType
            inputWrappedType.name == "com_nfl_glitr_data_mutation_VideoMutationInput"
            inputWrappedType.description == "Relay mutation input"
            inputWrappedType.fields.each { it -> it instanceof GraphQLInputObjectField }
            inputWrappedType.fields.name as Set == ["clientMutationId", "videoMutation"] as Set
            def GraphQLInputObjectField fieldDefInputType = (GraphQLInputObjectField) inputWrappedType.fields[1]
            fieldDefInputType.description == "Info meta data needed"
            fieldDefInputType.defaultValue == null
            fieldDefInputType.type instanceof GraphQLNonNull
            def inputFieldWrappedType = (GraphQLInputObjectType) ((GraphQLNonNull) fieldDefInputType.type).wrappedType
            inputFieldWrappedType.name == compatibleClassName(VideoMutationIn)
            inputFieldWrappedType.description == GlitrDescription.DEFAULT_DESCRIPTION
            inputFieldWrappedType.fields.name as Set == ["url", "title", "bitrateList"] as Set
            inputFieldWrappedType.fields[1].type instanceof GraphQLScalarType
            inputFieldWrappedType.fields[2].type instanceof GraphQLScalarType
            inputFieldWrappedType.fields[0].type instanceof GraphQLList
            inputFieldWrappedType.fields.defaultValue == [null, null, null]
    }

    def "Perform a mutation against mutationType"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .build()

            GraphQL graphQL = new GraphQL(glitr.getSchema());
        when:
            def result = graphQL.execute("""
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
        """);
            def data = result.data
            def errors = result.errors
        then:
            errors.empty
            (data as Map).saveVideoInfoMutation?.videoMutationPayload?.title == "My video title"
            (data as Map).saveVideoInfoMutation?.clientMutationId == "mutationId-Sx160620160639713-1"
    }
}
