package com.nfl.glitr.util

import com.nfl.glitr.Glitr
import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.calculator.QueryComplexityCalculator
import com.nfl.glitr.data.mutation.MutationType
import com.nfl.glitr.data.query.QueryType
import com.nfl.glitr.exception.GlitrException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class QueryComplexityCalculatorTest extends Specification {

    @Shared
    def queryComplexityCalculator;

    def setup() {
        queryComplexityCalculator = new QueryComplexityCalculator(200, 3, 50, 10);
    }

    @Unroll
    def "test query character score, case: #name"() {
        expect:
            int characterScore = queryComplexityCalculator.characterScore(query);
            characterScore == expectedResult

        where:
            query                              | name                                                || expectedResult
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
        |}'''.stripMargin()     | "query with 334 chars"                              || 334

            '''\
        |{
        |    playLists {
        |        tracks{
        |          trackId
        |        }
        |    }
        |}'''.stripMargin()     | "query with 69 chars"                               || 69

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
        |    }'''.stripMargin() | "query with 118 chars on the return mutation query" || 118
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
            query                              | name                                                                        || expectedResult
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
        |}'''.stripMargin()     | "query with 334 chars when max characters allowed is 200"                   || true

            '''\
        |{
        |    playLists {
        |        tracks{
        |          trackId
        |        }
        |    }
        |}'''.stripMargin()     | "query with 69 chars when max characters allowed is 200"                    || false

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
        |    }'''.stripMargin() | "query with 146 chars on the return mutation query when max allowed is 200" || false
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
        |}'''.stripMargin()     | "query with depth 2" || 2

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
        |}'''.stripMargin()     | "query with depth 3" || 3

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
        |}'''.stripMargin()     | "query with depth 4" || 4

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
        |    }'''.stripMargin() | "query with depth 2" || 2
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
            exception.getMessage().equals("Cannot parse query [@5,22:21='<EOF>',<EOF>,1:22] {playLists{playListId}")

        where:
            query = '''{playLists{playListId}'''.stripMargin()

    }

    @Unroll
    def "test query depth limit, case: #name"() {
        expect:
            boolean depthLimitExceeded = queryComplexityCalculator.depthLimitExceeded(query);
            depthLimitExceeded == expectedResult

        where:
            query                              | name                                                       || expectedResult
            '''\
        |{
        | playListId
        |}'''.stripMargin()     | "query with depth 0 when max depth allowed is 3"           || false

            '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()     | "query with depth 1 when max depth allowed is 3"           || false

            '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albums(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()     | "query with depth 2 when max depth allowed is 3"           || false

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
        |}'''.stripMargin()     | "query with depth 2 when max depth allowed is 3"           || false

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
        |}'''.stripMargin()     | "query with depth 3 when max depth allowed is 3"           || false

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
        |}'''.stripMargin()     | "query with depth 3 when max depth allowed is 3"           || false

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
        |}'''.stripMargin()     | "query with depth 4 when max depth allowed is 3"           || true

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
        |    }'''.stripMargin() | "mutation return query with depth 2 when max allowed is 3" || false
    }

    @Unroll
    def "test query depth calculation"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(new QueryComplexityCalculator(1, 1, 1, 1))
                    .build()

        when:
            def depthScore = glitr.getQueryComplexityCalculator().depthScore("""
                {
                    $query
                }
            """);
        then:
            depthScore == expectedScore
        where:
            query                                                                     || expectedScore
            "videosDepth{id}"                                                         || 1
            "videos{edges{node{depth{id}}}}"                                          || 2
            "videos{edges{node{children{edges{node{depth{id}}}}}}}"                   || 3
            "childScore{first{second{id}}}"                                           || 3
            "currentCollectionSize(first: 3){id}"                                     || 1
            "currentCollectionSize(first: 3){totalCollectionsSize(first: 3){id}}"     || 2
            "zZZVideos(first: 3){allVariablesComplexityFormula(first: 3){first{id}}}" || 3
            "duplicateVariables{first{second{id}}}"                                   || 3
            "incorrectVariableDeclaration{id}"                                        || 1
    }

    @Unroll
    def "test query depth calculation with ignored nodes"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(new QueryComplexityCalculator(1, 1, 1, 1))
                    .build()

        when:
            def depthScore = glitr.getQueryComplexityCalculator().depthScore("""
                {
                    $query
                }
            """);
        then:
            depthScore == expectedScore
        where:
            query                                    || expectedScore
            "ignore{id}"                             || 0
            "ignore{ignore{id}}"                     || 0
            "ignore{depth{id}}"                      || 1
            "videosDepth{ignore{id}}"                || 1
            "videos{edges{node{ignore{id}}}}"        || 1
            "videos{edges{node{ignore{depth{id}}}}}" || 2
    }

    @Unroll
    def "test query score, case: #name"() {
        expect:
            int queryScore = queryComplexityCalculator.queryScore(query, null);
            queryScore == expectedResult

        where:
            query                              | name                                        || expectedResult
            '''\
        |{
        | playListId
        |}'''.stripMargin()     | "query with a score of 0"                   || 0

            '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()     | "query with a score of 10"                  || 10

            '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albums(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()     | "query with a score of 40"                  || 40

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
        |}'''.stripMargin()     | "query with a score of 110"                 || 110

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
        |}'''.stripMargin()     | "query with a score of 70"                  || 70

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
        |}'''.stripMargin()     | "query with a score of 130"                 || 130

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
        |}'''.stripMargin()     | "query with a score of 100"                 || 100

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
        |    }'''.stripMargin() | "mutation return query with a score of 30"  || 30

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
        |    }'''.stripMargin() | "mutation return query with a score of 130" || 130


    }

    @Unroll
    def "test query score limit, case: #name"() {
        expect:
            boolean scoreLimitExceeded = queryComplexityCalculator.scoreLimitExceeded(query, null);
            scoreLimitExceeded == expectedResult

        where:
            query                              | name                                                || expectedResult
            '''\
        |{
        | playListId
        |}'''.stripMargin()     | "query with score 0 when max score allowed is 50"   || false

            '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()     | "query with score 10 when max score allowed is 50"  || false

            '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albums(first:3){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()     | "query with score 40 when max score allowed is 50"  || false

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
        |}'''.stripMargin()     | "query with score 210 when max score allowed is 50" || true


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
        |}'''.stripMargin()     | "query with score 60 when max score allowed is 50"  || true

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
        |}'''.stripMargin()     | "query with score 60 when max score allowed is 50"  || true

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
        |    }'''.stripMargin() | "query with score 30 when max score allowed is 50"  || false
    }

    @Unroll
    def "test validate query, case: #name"() {
        expect:
            queryComplexityCalculator.validate(query, null);

        where:
            query                              | name
            '''\
        |{
        | playListId
        |}'''.stripMargin()     | "query with a score of 0"

            '''\
        |{
        |    playLists {
        |        playListId
        |    }
        |}'''.stripMargin()     | "query with a score of 10"

            '''\
        |{
        |    playLists {
        |        playListId
        |        playListTitle
        |        albumsFirst(first:1){
        |           albumId
        |        }
        |    }
        |}'''.stripMargin()     | "query with a score of 50"

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
        |    }'''.stripMargin() | "mutation return query with a score of 30"
    }

    def "test check valid query with exception [characterLimitReached]"() {
        when:
            queryComplexityCalculator.validate(query, null)

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
            queryComplexityCalculator.validate(query, null)

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
            queryComplexityCalculator.validate(query, null)

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
            query                              | name                      || expectedResult
            '''\
        |{
        | viewer{
        |    test
        |  }
        |}'''.stripMargin()     | "query is not a mutation" || false

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
        |    }'''.stripMargin() | "query is a mutation"     || true

            '''\
        |{
        |    playLists {
        |        albumsFirst(first:30){
        |           mutation
        |           trackPick1{
        |              myMutation
        |           }
        |        }
        |    }
        |}'''.stripMargin()     | "query is not a mutation" || false

            '''\
        |myMutation {
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
        |    }'''.stripMargin() | "query is a mutation"     || false


    }

    @Unroll
    def "Calculate query complexity with default multiplier"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(new QueryComplexityCalculator(0, 0, 0, 1))
                    .build()

        when:
            def queryScore = glitr.getQueryComplexityCalculator().queryScore("""
            mutation {
                saveVideoInfoMutation(input: {
                    clientMutationId: \"mutationId-Sx160620160639713-1\"
                    videoMutation: {
                        title: "My video title"
                        bitrateList: [
                            {id: "1"},
                            {id: "2"},
                            {id: "3"}
                        ]
                    }
                }){
                    clientMutationId
                    videoMutationPayload {
                        title
                        $bitrate
                    }
                }
            }
        """, null);
        then:
            queryScore == expectedScore
        where:
            bitrate           || expectedScore
            "bitrateList{id}" || 5
            "_"               || 1
    }

    @Unroll
    def "Calculate query complexity with specified bitrateList multiplier = 4"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(new QueryComplexityCalculator(0, 0, 0, 0))
                    .build()

        when:
            def queryScore = glitr.getQueryComplexityCalculator().queryScore("""
            mutation {
                saveVideoInfoMutation(input: {
                    clientMutationId: "mutationId-Sx160620160639713-1"
                    videoMutation: {
                        title: "My video title"
                        bitrateList: [
                            {id: "1"},
                            {id: "2"},
                            {id: "3"}
                        ]
                    }
                }){
                    clientMutationId
                    videoMutationPayload {
                        title
                        $bitrate
                    }
                }
            }
        """, null);
        then:
            queryScore == expectedScore
        where:
            bitrate           || expectedScore
            "bitrateList{id}" || 4
            "id"              || 0
    }


    @Unroll
    def "Invoke multiplier extractor with and without pagination support"() {
        setup:
            //def qcc = new QueryComplexityCalculator(0, 0, 0,0)
            def queryComplexityCalculator = Spy(QueryComplexityCalculator)

            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(queryComplexityCalculator)
                    .build()

        when:

            glitr.getQueryComplexityCalculator().queryScore(query, null);
        then:
            invocationTimes * queryComplexityCalculator.extractMultiplierFromListField(*_)

        where:
            query              || invocationTimes
            """
            {
                videos {
                    edges {
                        node {
                            bitrateList {
                                id
                            }
                        }
                    }
                }
            }
            """ || 3

    }

    @Unroll
    def "Calculate query complexity with specified formula"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(new QueryComplexityCalculator(1, 1, 1, 1))
                    .build()

        when:
            def queryScore = glitr.getQueryComplexityCalculator().queryScore("""
                $variables {
                    $query
                }
            """, [limit: 3]);
        then:
            queryScore == expectedScore
        where:
            variables             || query                                                                           || expectedScore
            ""                    || "videosDepth{id}"                                                               || 1
            ""                    || "videos{edges{node{depth{id}}}}"                                                || 3
            ""                    || "videos{edges{node{children{edges{node{depth{id}}}}}}}"                         || 5
            ""                    || "videos{edges{node{fragments{edges{node{... on Video{depth{id}}}}}}}}"          || 5
            ""                    || "videos{edges{node{children{pageInfo{total}edges{node{depth{id}}}}}}}"          || 5
            ""                    || "childScore{first{second{id}}}"                                                 || 4
            ""                    || "currentCollectionSize(first: 3){id}"                                           || 3
            ""                    || "currentCollectionSize(first: 3){totalCollectionsSize(first: 3){id}}"           || 9
            'query($limit: Int!)' || 'currentCollectionSize(first: $limit){totalCollectionsSize(first: $limit){id}}' || 9
            ""                    || "zZZVideos(first: 3){allVariablesComplexityFormula(first: 3){first{id}}}"       || 69
            ""                    || "duplicateVariables{first{second{id}}}"                                         || 8
            ""                    || "incorrectVariableDeclaration{id}"                                              || 0
            ""                    || "abstract{url}"                                                                 || 6
    }

    def "Calculate query complexity with fragment spread"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(new QueryComplexityCalculator(1, 1, 1, 1))
                    .build()

        when:
            def queryScore = glitr.getQueryComplexityCalculator().queryScore("""
                {
                    $query
                }

                $fragments
            """, null);
        then:
            queryScore == 25
        where:
            query = '''
                videos(first:5){
                  edges{
                    node{
                      ...QueryTypeFragment
                    }
                  }
                }'''


            fragments = '''
                fragment QueryTypeFragment on QueryType{
                  id
                  otherVideos{
                    edges{
                      node{
                        ...VideoFragment
                      }
                    }
                  }
                }
                
                fragment VideoFragment on Video{
                  depth{id}
                }'''
    }

    @Unroll
    def "Calculate query complexity with ignored nodes"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr()
                    .withRelay()
                    .withQueryRoot(new QueryType())
                    .withMutationRoot(new MutationType())
                    .withObjectMapper(SerializationUtil.objectMapper)
                    .withQueryComplexityCalculator(new QueryComplexityCalculator(1, 1, 1, 1))
                    .build()

        when:
            def queryScore = glitr.getQueryComplexityCalculator().queryScore("""
                {
                    $query
                }
            """, null);
        then:
            queryScore == expectedScore
        where:
            query                                    || expectedScore
            "ignore{id}"                             || 0
            "ignore{ignore{id}}"                     || 0
            "ignore{depth{id}}"                      || 1
            "videosDepth{ignore{id}}"                || 1
            "videos{edges{node{ignore{id}}}}"        || 1
            "videos{edges{node{ignore{depth{id}}}}}" || 3
    }
}