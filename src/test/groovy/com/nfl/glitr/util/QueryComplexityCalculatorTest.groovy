package com.nfl.glitr.util

import com.nfl.glitr.exception.GlitrException
import com.nfl.glitr.util.complexity.QueryComplexityCalculator
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class QueryComplexityCalculatorTest extends Specification {

    @Shared queryComplexityCalculator;

    def setup() {
        queryComplexityCalculator = new QueryComplexityCalculator(200, 3, 10);
    }

    @Unroll
    def "test query character score, case: #name"() {
        expect:
        int characterScore = queryComplexityCalculator.characterScore(query);
        characterScore == expectedResult

        where:
        query                              | name                 || expectedResult
        '''\
        |{
        |    viewer {
        |        playLists(first:25) {
        |            playListId
        |            playListTitle
        |            albums{
        |                albumId
        |                albumTitle
        |                tracks(first: 35) {
        |                   trackId
        |                   trackTitle
        |                   runTime
        |                }
        |            }
        |        }
        |    }
        |}'''.stripMargin()            | "query with 334 chars" || 334

        '''\
        |{
        |    playLists {
        |        tracks{
        |          trackId
        |        }
        |    }
        |}'''.stripMargin()            | "query with 69 chars" || 69
    }

    def "test query character score with exception [empty query]"() {
        when:
        queryComplexityCalculator.characterScore(query)

        then:
        def exception = thrown(GlitrException)
        exception.getMessage().equals("query cannot be null or empty")

        where:
        query = '''
        '''.stripMargin()
    }

    @Unroll
    def "test query character limit, case: #name"() {
        expect:
        boolean characterLimitExceeded = queryComplexityCalculator.characterLimitExceeded(query);
        characterLimitExceeded == expectedResult

        where:
        query                              | name                 || expectedResult
        '''\
        |{
        |    viewer {
        |        playLists(first:25) {
        |            playListId
        |            playListTitle
        |            albums{
        |                albumId
        |                albumTitle
        |                tracks(first: 35) {
        |                   trackId
        |                   trackTitle
        |                   runTime
        |                }
        |            }
        |        }
        |    }
        |}'''.stripMargin()            | "query with 334 chars when max characters allowed is 200" || true

        '''\
        |{
        |    playLists {
        |        tracks{
        |          trackId
        |        }
        |    }
        |}'''.stripMargin()            | "query with 69 chars when max characters allowed is 200" || false
    }

    @Unroll
    def "test query depth score, case: #name"() {
        expect:
        int depthScore = queryComplexityCalculator.depthScore(query);
        depthScore == expectedResult

        where:
        query                              | name                 || expectedResult
        '''\
        |{
        |    playLists {
        |        tracks{
        |          trackId
        |        }
        |    }
        |}'''.stripMargin()            | "query with depth 2" || 2

        '''\
        |{
        |    playLists {
        |        tracks{
        |          trackId
        |          trackInfo{
        |            runtime
        |          }
        |        }
        |    }
        |}'''.stripMargin()            | "query with depth 3" || 3

        '''\
        |{
        |    viewer {
        |        playLists(first:25) {
        |            playListId
        |            playListTitle
        |            albums{
        |                albumId
        |                albumTitle
        |                tracks(first: 35) {
        |                   trackId
        |                   trackTitle
        |                   runTime
        |                }
        |            }
        |        }
        |    }
        |}'''.stripMargin()            | "query with depth 4" || 4

    }

    def "test query depth score with exception [empty query]"() {
        when:
        queryComplexityCalculator.depthScore(query)

        then:
        def exception = thrown(GlitrException)
        exception.getMessage().equals("query cannot be null or empty")

        where:
        query = '''
        '''.stripMargin()
    }

    def "test query depth score with exception [malformed query with missing braces]"() {
        when:
        queryComplexityCalculator.depthScore(query)

        then:
        def exception = thrown(GlitrException)
        exception.getMessage().equals("Cannot parse query {playLists{playListId}")

        where:
        query = '''{playLists{playListId}'''.stripMargin()

    }

    def "test query depth score with exception [no mutations allowed]"() {
        when:
        queryComplexityCalculator.depthScore(query)

        then:
        def exception = thrown(GlitrException)
        exception.getMessage().equals("Cannot find query node 'OperationDefinition' or query is a 'MUTATION'")

        where:
        query = '''\
        |mutation {
        |      eventMtn(input: {
        |        clientMutationId: "abc"
        |        event: {
        |          id: "abc"
        |          propertyId: "32142020-4c41-5f53-5441-4c4c494f4e53"
        |        }
        |      }) {
        |        event {
        |          id
        |          property {
        |            id
        |            franchises {
        |               id
        |               name
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()

    }

    @Unroll
    def "test query depth limit, case: #name"() {
        expect:
        boolean depthLimitExceeded = queryComplexityCalculator.depthLimitExceeded(query);
        depthLimitExceeded == expectedResult

        where:
        query                              | name                 || expectedResult
        '''\
        |{
        | playListId
        |}'''.stripMargin()                | "query with depth 0 when max depth allowed is 3" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()               | "query with depth 1 when max depth allowed is 3" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albums(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()               | "query with depth 2 when max depth allowed is 3" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |        }
        |        albumsSecond(first:6){
        |           albumId
        |        }
        |        albumsThird{
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()               | "query with depth 2 when max depth allowed is 3" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |           tracks{
        |              trackId
        |           }
        |        }
        |    }
        |}'''.stripMargin()               | "query with depth 3 when max depth allowed is 3" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |           trackPick1{
        |              trackId
        |           }
        |           trackPick2{
        |              trackid
        |           }
        |        }
        |    }
        |}'''.stripMargin()               | "query with depth 3 when max depth allowed is 3" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |           trackPick1{
        |              trackId
        |              trackInfo{
        |                runTimeSecs
        |                artist
        |              }
        |           }
        |        }
        |    }
        |}'''.stripMargin()               | "query with depth 4 when max depth allowed is 3" || true
    }

    @Unroll
    def "test query score, case: #name"() {
        expect:
        int queryScore = queryComplexityCalculator.queryScore(query);
        queryScore == expectedResult

        where:
        query                              | name                 || expectedResult
        '''\
        |{
        | playListId
        |}'''.stripMargin()                | "query with a score of 0" || 0

        '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()               | "query with a score of 10" || 10

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albums(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()               | "query with a score of 16" || 16

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |        }
        |        albumsSecond(first:6){
        |           albumId
        |        }
        |        albumsThird{
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()               | "query with a score of 48" || 48

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |           tracks{
        |              trackId
        |           }
        |        }
        |    }
        |}'''.stripMargin()               | "query with a score of 46" || 46

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |           trackPick1{
        |              trackId
        |              trackInfo(first:2){
        |                runTimeSecs
        |                artist
        |              }
        |           }
        |        }
        |    }
        |}'''.stripMargin()               | "query with a score of 54" || 54

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |           trackPick1{
        |              trackId
        |           }
        |           trackPick2{
        |              trackid
        |           }
        |        }
        |    }
        |}'''.stripMargin()               | "query with a score of 76" || 76

    }

    @Unroll
    def "test check and retrieve query score, case: #name"() {
        expect:
        int queryScore = queryComplexityCalculator.checkAndRetrieveQueryScore(query);
        queryScore == expectedResult

        where:
        query                          | name                       || expectedResult
        '''\
        |{
        | playListId
        |}'''.stripMargin()            | "query with a score of 0"  || 0

        '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()            | "query with a score of 10" || 10

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()               | "query with a score of 16" || 16
    }

    def "test check and retrieve query score with exception [characterLimitReached]"() {
        when:
        queryComplexityCalculator.checkAndRetrieveQueryScore(query)

        then:
        def exception = thrown(GlitrException)
        exception.getMessage().equals("query length has exceeded the maximum of 200 characters.")

        where:
        query = '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |        }
        |        albumsSecond(first:6){
        |           albumId
        |        }
        |        albumsThird{
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()

    }

    def "test check and retrieve query score with exception [depthLimitReached]"() {
        when:
        queryComplexityCalculator.checkAndRetrieveQueryScore(query)

        then:
        def exception = thrown(GlitrException)
        exception.getMessage().equals("query depth has exceeded the maximum depth level of 3.")

        where:
        query = '''\
        |{
        | list{
        |    d1{
        |      d2{
        |        d3{
        |          d4{
        |           floor
        |          }
        |        }
        |      }
        |    } 
        | }
        |}'''.stripMargin()

    }

}