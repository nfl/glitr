package com.nfl.glitr.registry

import com.nfl.glitr.Glitr
import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.query.additionalTypes.Cyborg
import com.nfl.glitr.data.query.additionalTypes.Man
import com.nfl.glitr.data.query.additionalTypes.QueryRoot
import com.nfl.glitr.util.SerializationUtil
import graphql.TypeResolutionEnvironment
import spock.lang.Specification

class GlitrAdditionalTypesTest extends Specification {

    def "Register Additional types"() {
        when: "first discovery"
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryRoot(new QueryRoot())
                    .build()

            def typeResolutionEnvMan = new TypeResolutionEnvironment(new Man(), null, null, null, null, null)
            def typeResolutionEnvCyborg = new TypeResolutionEnvironment(new Cyborg(), null, null, null, null, null)

        then: "incidentally Man and Cyborg by default have not been discovered"
            glitr.typeRegistry.getType(typeResolutionEnvMan) == null
            glitr.typeRegistry.getType(typeResolutionEnvCyborg) == null

        when: "add additional types to type registry and reload the schema"
            glitr.typeRegistry.lookup(Man.class)
            glitr.typeRegistry.lookup(Cyborg.class)
            glitr.reloadSchema(QueryRoot.class, null, null)
        then: "Man and Cyborg are now part of the schema"
            glitr.typeRegistry.getType(typeResolutionEnvMan) != null
            glitr.typeRegistry.getType(typeResolutionEnvCyborg) != null
            glitr.schema.getType(Man.class.simpleName) != null
            glitr.schema.getType(Cyborg.class.simpleName) != null
    }
}
