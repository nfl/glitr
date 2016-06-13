package com.nfl.dm.shield.graphql.registry.datafetcher.query

import graphql.Scalars
import graphql.schema.DataFetchingEnvironment
import graphql.schema.PropertyDataFetcher
import spock.lang.Specification

class CompositeDataFetcherTest extends Specification {

    def propertyDataFetcher = new PropertyDataFetcher("title")
    def overrideDataFetcher = new OverrideDataFetcher("title", new Override())
    def env = new DataFetchingEnvironment(new DummyClass(), null, null, null, Scalars.GraphQLString, null, null);

    def "Should iterate over dataFetchers until override gets called & return null when not found"() {
        expect:
        new CompositeDataFetcher(propertyDataFetcher).get(env) == null
        new CompositeDataFetcher([propertyDataFetcher]).get(env) == null
        new CompositeDataFetcher(propertyDataFetcher, overrideDataFetcher).get(env) == "Title"
        new CompositeDataFetcher([propertyDataFetcher, overrideDataFetcher]).get(env) == "Title"
    }

    class Override {
        public String getTitle(DataFetchingEnvironment environment) {
            return "Title";
        }
    }

    class DummyClass {
        public String getName() {
            return "Name";
        }
    }
}
