package com.nfl.glitr.registry.datafetcher.query.batched

import com.nfl.glitr.registry.datafetcher.query.CompositeDataFetcher
import com.nfl.glitr.registry.datafetcher.query.OverrideDataFetcher
import graphql.execution.batched.Batched
import graphql.execution.batched.BatchedDataFetcher
import graphql.execution.batched.UnbatchedDataFetcher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.PropertyDataFetcher
import spock.lang.Specification

class CompositeDataFetcherFactoryTest extends Specification{

    PropertyDataFetcher propertyDataFetcher = new PropertyDataFetcher('attribution')
    OverrideDataFetcher overrideDataFetcher = new OverrideDataFetcher('video', Video.class)
    OverrideDataFetcher overrideDataFetcherNullOverrideMethod = new OverrideDataFetcher('notFoundMethod', Video.class)
    OverrideDataFetcher overrideDataFetcherBatchedOverrideMethod = new OverrideDataFetcher('title', Video.class)

    BatchedDataFetcher batchedDataFetcher = new UnbatchedDataFetcher(new PropertyDataFetcher('copyright'))


    def "All DF are batched"() {
        when:"all the data fetchers are batched (exclude PropertyDataFetcher or OverrideDataFetcher)"
            List<DataFetcher> dataFetchers = [propertyDataFetcher, batchedDataFetcher]
            def df = (BatchedCompositeDataFetcher) CompositeDataFetcherFactory.create(dataFetchers)
        then:"it will create a BatchedCompositeDataFetcher"
            df.fetchers.size() == 2
            df.fetchers[0] instanceof BatchedDataFetcher
            df.fetchers[1] instanceof BatchedDataFetcher
    }

    def "All DF are batched with batched override method"() {
        when:
            List<DataFetcher> dataFetchers = [propertyDataFetcher, batchedDataFetcher, overrideDataFetcherBatchedOverrideMethod]
            def df = (BatchedCompositeDataFetcher) CompositeDataFetcherFactory.create(dataFetchers)
        then:"it will create a BatchedCompositeDataFetcher and has transformed PropertyDF and OverrideDF into batched ones"
            df.fetchers.size() == 3
            df.fetchers[0] instanceof BatchedDataFetcher
            df.fetchers[1] instanceof BatchedDataFetcher
            df.fetchers[2] instanceof BatchedDataFetcher
    }

    def "All DF are un-batched"() {
        when:
            List<DataFetcher> dataFetchers = [propertyDataFetcher, overrideDataFetcher, overrideDataFetcherNullOverrideMethod]
            def df = (CompositeDataFetcher) CompositeDataFetcherFactory.create(dataFetchers)
        then:"it will create a CompositeDataFetcher"
            df.fetchers.size() == 2
            df.fetchers[0] instanceof PropertyDataFetcher
            df.fetchers[1] instanceof OverrideDataFetcher
    }

    def "Both batched and un-batched present should throw exception"() {
        when:
            List<DataFetcher> dataFetchers = [propertyDataFetcher, batchedDataFetcher, overrideDataFetcher]
            CompositeDataFetcherFactory.create(dataFetchers)
        then:
            thrown(IllegalArgumentException)
    }


    class Video {
        String title
        Video video

        @Batched
        String getTitle(DataFetchingEnvironment environment) {
            return title
        }

        Video getVideo() {
            return video
        }

        Video getVideo(DataFetchingEnvironment environment) {
            return video
        }
    }
}
