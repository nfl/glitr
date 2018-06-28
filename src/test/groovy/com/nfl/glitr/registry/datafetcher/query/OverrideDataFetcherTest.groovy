package com.nfl.glitr.registry.datafetcher.query

import graphql.Scalars
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentBuilder
import spock.lang.Specification

class OverrideDataFetcherTest extends Specification {
    def execCtx = ExecutionContextBuilder.newExecutionContextBuilder()
            .executionId(ExecutionId.generate())
            .build();
    def env = DataFetchingEnvironmentBuilder.newDataFetchingEnvironment()
            .source(new DummyClass())
            .fieldType(Scalars.GraphQLString)
            .executionContext(execCtx)
            .build()

    def "override method in Override class"() {
        expect:
        new OverrideDataFetcher("title", new Override()).get(env) == "Title"
        new OverrideDataFetcher("notfound", new Override()).get(env) == null
        new OverrideDataFetcher("title", Override.class).get(env) == null
        new OverrideDataFetcher("notfound", Override.class).get(env) == null /* this one should fail silently and return null */
    }

    def "override method inside DummyClass"() {
        expect:
        new OverrideDataFetcher("nameOverride", DummyClass.class).get(env) == "Override inside class"
        new OverrideDataFetcher("notFound", DummyClass.class).get(env) == null
    }

    public class Override {
        public String getTitle(DataFetchingEnvironment environment) {
            return "Title";
        }
    }

    public class DummyClass {
        public String getName() {
            return "Name";
        }

        public String getNameOverride(DataFetchingEnvironment environment) {
            return "Override inside class";
        }
    }
}
