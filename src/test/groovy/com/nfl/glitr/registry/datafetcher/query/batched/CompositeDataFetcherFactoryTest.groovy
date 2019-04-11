package com.nfl.glitr.registry.datafetcher.query.batched

import com.nfl.glitr.registry.datafetcher.query.CompositeDataFetcher
import com.nfl.glitr.registry.datafetcher.query.OverrideDataFetcher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.PropertyDataFetcher
import spock.lang.Specification

class CompositeDataFetcherFactoryTest extends Specification{

    PropertyDataFetcher propertyDataFetcher = new PropertyDataFetcher('attribution')
    OverrideDataFetcher overrideDataFetcher = new OverrideDataFetcher('video', Video.class)
    OverrideDataFetcher overrideDataFetcherNullOverrideMethod = new OverrideDataFetcher('notFoundMethod', Video.class)


    def "All DF are present"() {
        when:
            List<DataFetcher> dataFetchers = [propertyDataFetcher, overrideDataFetcher, overrideDataFetcherNullOverrideMethod]
            def df = (CompositeDataFetcher) CompositeDataFetcherFactory.create(dataFetchers)
        then:"it will create a CompositeDataFetcher"
            df.fetchers.size() == 2
            df.fetchers[0] instanceof PropertyDataFetcher
            df.fetchers[1] instanceof OverrideDataFetcher
    }

    class Video {
        String title
        Video video

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
