package com.nfl.dm.shield.graphql.registry

import com.nfl.dm.shield.graphql.GlitrBuilder

class TypeRegistryIntegrationTest {

    def glitr = GlitrBuilder.newGlitrWithRelaySupport().build()
    def typeRegistry = glitr.typeRegistry
    def relayHelper = glitr.relayHelper


    def "init registry"() {

    }

}
