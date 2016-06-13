package com.nfl.dm.shield.graphql.relay

import com.fasterxml.jackson.databind.ObjectMapper
import com.nfl.dm.shield.graphql.registry.TypeRegistry
import spock.lang.Specification

class RelayHelperTest extends Specification {

    def typeRegistry = new TypeRegistry()
    def relayHelper = new RelayHelper(typeRegistry, null, new ObjectMapper())

    //TODO: once finalized logic add tests
}
