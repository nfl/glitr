package com.nfl.glitr.util

import com.nfl.glitr.exception.GlitrException
import com.nfl.glitr.util.complexity.QueryComplexityCalculator
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class QueryComplexityCalculatorTest extends Specification {

    @Shared queryComplexityCalculator;

    def setup() {
        queryComplexityCalculator = new QueryComplexityCalculator(200, 3, 50,10);
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

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackId
        |            trackInfo {
        |               id
        |               name
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "query with 186 chars on the return mutation query" || 186
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

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackInfo {
        |               id
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "query with 146 chars on the return mutation query when max allowed is 200" || false
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

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackId
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "query with depth 2" || 2
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

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackId
        |            trackInfo {
        |               id
        |               name
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "mutation return query with depth 2 when max allowed is 3" || false
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

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackId
        |            trackInfo {
        |               id
        |               name
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "mutation return query with a score of 60" || 60

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks(first:2) {
        |            trackId
        |            trackInfo(first:5) {
        |               id
        |               name
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "mutation return query with a score of 29" || 29


    }

    @Unroll
    def "test query score limit, case: #name"() {
        expect:
        boolean scoreLimitExceeded = queryComplexityCalculator.scoreLimitExceeded(query);
        scoreLimitExceeded == expectedResult

        where:
        query                              | name                 || expectedResult
        '''\
        |{
        | playListId
        |}'''.stripMargin()                | "query with score 0 when max score allowed is 50" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()               | "query with score 10 when max score allowed is 50" || false

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albums(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()               | "query with score 16 when max score allowed is 50" || false

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
        |}'''.stripMargin()               | "query with score 48 when max score allowed is 50" || false


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
        |}'''.stripMargin()               | "query with score 76 when max score allowed is 50" || true

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
        |}'''.stripMargin()               | "query with score 86 when max score allowed is 50" || true

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackId
        |            trackInfo {
        |               id
        |               name
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "query with score 60 when max score allowed is 50" || true
    }

    @Unroll
    def "test validate query, case: #name"() {
        expect:
       queryComplexityCalculator.validate(query);

        where:
        query                          | name
        '''\
        |{
        | playListId
        |}'''.stripMargin()            | "query with a score of 0"

        '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()            | "query with a score of 10"

        '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()               | "query with a score of 16"

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackId
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "mutation return query with a score of 30"
    }

    def "test check valid query with exception [characterLimitReached]"() {
        when:
        queryComplexityCalculator.validate(query)

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

    def "test check valid query with exception [depthLimitReached]"() {
        when:
        queryComplexityCalculator.validate(query)

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

    def "test check valid query with exception [scoreLimitReached]"() {
        when:
        queryComplexityCalculator.validate(query)

        then:
        def exception = thrown(GlitrException)
        exception.getMessage().equals("query score has exceeded the maximum score level of 50.")

        where:
        query = '''\
        |{
        |    playLists {
        |        albumsFirst(first:30){
        |           albumId
        |           trackPick1{
        |              trackId
        |           }
        |        }
        |    }
        |}'''.stripMargin()

    }

    @Unroll
    def "test check if query is mutation, case: #name"() {
        expect:
        boolean isMutation = queryComplexityCalculator.isMutation(query);
        isMutation == expectedResult

        where:
        query                          | name                       || expectedResult
        '''\
        |{
        | viewer{
        |    test
        |  }
        |}'''.stripMargin()            | "query is not a mutation"  || false

        '''\
        |mutation {
        |      playListMtn(input: {
        |        clientId: "abc"
        |        playList: {
        |          playListId: "abc"
        |          title: "MyPlaylist"
        |        }
        |      }) {
        |        playList {
        |          playListId
        |          tracks {
        |            trackId
        |            trackInfo {
        |               id
        |               name
        |            }
        |          }
        |        }
        |      }
        |    }'''.stripMargin()       | "query is a mutation" || true


    }
}