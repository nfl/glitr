package com.nfl.glitr.registry.datafetcher.query

import graphql.Scalars
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentBuilder
import graphql.schema.PropertyDataFetcher
import spock.lang.Specification

class CompositeDataFetcherTest extends Specification {

    def propertyDataFetcher = new PropertyDataFetcher("title")
    def overrideDataFetcher = new OverrideDataFetcher("title", new Override())
    def execCtx = ExecutionContextBuilder.newExecutionContextBuilder()
                                        .executionId(ExecutionId.generate())
                                        .build();
    def env = DataFetchingEnvironmentBuilder.newDataFetchingEnvironment()
                                        .source(new DummyClass())
                                        .fieldType(Scalars.GraphQLString)
                                        .executionContext(execCtx)
                                        .build()

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
