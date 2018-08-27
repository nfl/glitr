package com.nfl.glitr.registry

import com.nfl.glitr.Glitr
import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.query.PublishType
import com.nfl.glitr.data.query.additionalTypes.QueryRoot
import com.nfl.glitr.util.SerializationUtil
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import spock.lang.Specification

class GlitrArgumentsEnumTest extends Specification {

    def "Inspect GlitrArgument Default Enumeration Tests"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryRoot(new QueryRoot())
                    .build()
        when:
            GraphQLObjectType type = (GraphQLObjectType) glitr.typeRegistry.lookup(QueryRoot.class)
        then: "Video has empty default value"
            ((GraphQLNonNull) type.getFieldDefinition("defaultVideo").getArgument("video_default").getType()).wrappedType instanceof GraphQLScalarType
            type.getFieldDefinition("defaultVideo").getArgument("video_default").defaultValue == null;
        then: "Video has non-enum default value"
            ((GraphQLNonNull) type.getFieldDefinition("nonEnumVideo").getArgument("video_non_enum").getType()).wrappedType instanceof GraphQLScalarType
            type.getFieldDefinition("nonEnumVideo").getArgument("video_non_enum").defaultValue.equals("defaultTest");
        then: "Video has enum default value"
            ((GraphQLNonNull) type.getFieldDefinition("enumVideo").getArgument("video_enum").getType()).wrappedType instanceof GraphQLEnumType
            type.getFieldDefinition("enumVideo").getArgument("video_enum").getDefaultValue() instanceof PublishType
        then: "Video has enum incorrect default value"
            ((GraphQLNonNull) type.getFieldDefinition("enumFailVideo").getArgument("video_enum_fail").getType()).wrappedType instanceof GraphQLEnumType
            type.getFieldDefinition("enumFailVideo").getArgument("video_enum_fail").getDefaultValue() == null
    }
}