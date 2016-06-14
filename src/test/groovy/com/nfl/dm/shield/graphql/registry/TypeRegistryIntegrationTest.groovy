package com.nfl.dm.shield.graphql.registry

import com.fasterxml.jackson.databind.ObjectMapper
import com.nfl.dm.shield.graphql.relay.RelayHelper
import graphql.relay.Relay

class TypeRegistryIntegrationTest {

    def typeRegistry = TypeRegistryBuilder.newTypeRegistry().build()
    def relayHelper = new RelayHelper(new Relay(), typeRegistry, null, new ObjectMapper())


    def "init registry"() {

    }

}
