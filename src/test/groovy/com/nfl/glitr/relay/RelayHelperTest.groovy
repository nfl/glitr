package com.nfl.glitr.relay

import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.query.QueryType
import com.nfl.glitr.util.SerializationUtil
import spock.lang.Specification
import spock.lang.Unroll

class RelayHelperTest extends Specification {

    def glitr = GlitrBuilder.newGlitr()
            .withRelay()
            .withObjectMapper(SerializationUtil.objectMapper)
            .withQueryRoot(new QueryType()).build()
    def typeRegistry = glitr.typeRegistry
    def relayHelper = glitr.relayHelper


    @SuppressWarnings("GroovyPointlessBoolean")
    @Unroll
    def "test build connection pagination"() {
        expect:
            testPaging(offset, totalCount, hasNextPage, hasPrevPage, resultSize, previousPageOffset)

        where:
            offset || totalCount || hasNextPage || hasPrevPage || resultSize || previousPageOffset
            0      || 12         || true        || false       || 10         || null
            1      || 12         || false       || true        || 10         || null
            2      || 12         || false       || true        || 10         || null
            7      || 12         || false       || true        || 5          || null
            69     || 71         || false       || true        || 2          || 58
            70     || 71         || false       || true        || 1          || 59
            1      || 1          || false       || true        || 0          || null
            50     || 50         || false       || true        || 0          || 49
            11     || 12         || false       || true        || 1          || 0
            0      || 0          || false       || false       || 0          || null
            0      || 1          || false       || false       || 1          || null
            1      || 1          || false       || true        || 0          || null
            25     || 50         || true        || true        || 10         || 14
            10     || 50         || true        || true        || 10         || null
    }

    void testPaging(def offset, def totalCount, def hasNext, def hasPrev, def resultSize, def previousPageOffset) {
        def items = []
        def skipItem = 10
        for (def i = 0; i < skipItem; i++) {
            if ((i + offset) < totalCount) {
                items.add(i + offset)
            }
        }

        def connection = RelayHelper.buildConnection(items, offset, skipItem, totalCount)
        assert (connection.pageInfo.hasNextPage == hasNext)
        assert (connection.pageInfo.hasPreviousPage == hasPrev)
        assert (connection.edges.size() == resultSize)

        def pageInfoWithTotal = (PageInfoWithTotal) connection.pageInfo
        assert pageInfoWithTotal.total == totalCount

        if (resultSize > 0) {
            assert (connection.pageInfo.startCursor?.value == RelayHelper.createCursor(offset))
            assert (connection.pageInfo.endCursor?.value == RelayHelper.createCursor(offset + resultSize - 1))


            assert (pageInfoWithTotal.previousPageStartCursor?.value == (previousPageOffset == null ? null : RelayHelper.createCursor(previousPageOffset)))



        }

        connection.edges.forEach({
            assert (it.cursor.value == RelayHelper.createCursor(it.node))
        })
    }
}
