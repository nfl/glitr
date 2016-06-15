package com.nfl.dm.shield.graphql.relay

import com.nfl.dm.shield.graphql.GlitrBuilder
import spock.lang.Specification

class RelayHelperTest extends Specification {

    def glitr = GlitrBuilder.newGlitrWithRelaySupport().build()
    def typeRegistry = glitr.typeRegistry
    def relayHelper = glitr.relayHelper

    //TODO: once finalized logic add tests
}
