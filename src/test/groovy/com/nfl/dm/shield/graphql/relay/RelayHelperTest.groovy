package com.nfl.dm.shield.graphql.relay

import com.fasterxml.jackson.databind.ObjectMapper
import com.nfl.dm.shield.graphql.registry.TypeRegistryBuilder
import graphql.relay.Relay
import spock.lang.Specification

class RelayHelperTest extends Specification {

    def typeRegistry = TypeRegistryBuilder.newTypeRegistry().build()
    def relayHelper = new RelayHelper(new Relay(), typeRegistry, null, new ObjectMapper())

    //TODO: once finalized logic add tests
}
