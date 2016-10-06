package com.nfl.glitr.relay

import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.query.QueryType
import spock.lang.Specification

class RelayHelperTest extends Specification {

    def glitr = GlitrBuilder.newGlitrWithRelaySupport().withQueryRoot(new QueryType()).build()
    def typeRegistry = glitr.typeRegistry
    def relayHelper = glitr.relayHelper


    @SuppressWarnings("GroovyPointlessBoolean")
    def "test build connection pagination"() {
        expect:
        testPaging(toSkip, totalCount, hasNextPage, hasPrevPage, resultSize)

        where:
        toSkip  ||  totalCount      ||  hasNextPage     ||  hasPrevPage ||  resultSize
        0       ||  12              ||  true            ||  false       ||  10
        1       ||  12              ||  false           ||  true        ||  10
        2       ||  12              ||  false           ||  true        ||  10
        7       ||  12              ||  false           ||  true        ||  5
        69      ||  71              ||  false           ||  true        ||  2
        70      ||  71              ||  false           ||  true        ||  1
        1       ||  1               ||  false           ||  true        ||  0
        50      ||  50              ||  false           ||  true        ||  0
        11      ||  12              ||  false           ||  true        ||  1
        0       ||  0               ||  false           ||  false       ||  0
        0       ||  1               ||  false           ||  false       ||  1
        1       ||  1               ||  false           ||  true        ||  0
    }

    public void testPaging(def toSkip, def totalCount, def hasNext, def hasPrev, def resultSize) {
        def items = []
        for (def i = 0; i < 10; i++) {
            if ((i + toSkip) < totalCount) {
                items.add(i + toSkip);
            }
        }

        def connection = RelayHelper.buildConnection(items, toSkip, totalCount);
        assert (connection.pageInfo.hasNextPage == hasNext)
        assert (connection.pageInfo.hasPreviousPage == hasPrev)
        assert (connection.edges.size() == resultSize)

        def pageInfoWithTotal = (PageInfoWithTotal) connection.pageInfo
        assert pageInfoWithTotal.total == totalCount

        if (resultSize > 0) {
            assert (connection.pageInfo.startCursor?.value == graphql.relay.Base64.toBase64("simple-cursor${toSkip}"))
            assert (connection.pageInfo.endCursor?.value == graphql.relay.Base64.toBase64("simple-cursor${toSkip + resultSize - 1}"))
        }

        connection.edges.forEach({
            assert (it.cursor.value == graphql.relay.Base64.toBase64("simple-cursor${it.node}"));
        });
    }
}
