package com.nfl.dm.shield.graphql.registry

import com.fasterxml.jackson.databind.ObjectMapper
import com.nfl.dm.shield.graphql.relay.RelayHelper

class TypeRegistryIntegrationTest {

    def typeRegistry = new TypeRegistry()
    def relayHelper = new RelayHelper(typeRegistry, null, new ObjectMapper())


    def "init registry"() {

    }

}
