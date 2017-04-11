package com.nfl.glitr.registry

import com.nfl.glitr.Glitr
import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.additionalScalars.CustomScalar
import com.nfl.glitr.data.additionalScalars.Root
import graphql.Scalars
import spock.lang.Specification

class GlitrRegisterAdditionalScalars extends Specification {

    def "Register custom scalars"() {
        when: "first discovery"
        Glitr glitr = GlitrBuilder.newGlitr()
                .withQueryRoot(new Root())
                .addCustomScalar(CustomScalar.class, Scalars.GraphQLString)
                .build()
        then: "make sure the scalar has been registered correctly as a GraphQLString"
        glitr.typeRegistry.getType(new Root()).getFieldDefinition("scalar").type.name == Scalars.GraphQLString.name
    }

    def "Register twice the same custom scalar should fail"() {
        when: "first discovery"
        Glitr glitr = GlitrBuilder.newGlitr()
                .withQueryRoot(new Root())
                .addCustomScalar(CustomScalar.class, Scalars.GraphQLString)
                .addCustomScalar(CustomScalar.class, Scalars.GraphQLInt)
                .build()
        then: "make sure Glitr doesn't let the user attempt to register two scalars for the same java type."
        def e = thrown(IllegalArgumentException)
        e.getMessage().contains("You have previously registered the following Java type")
    }
}
