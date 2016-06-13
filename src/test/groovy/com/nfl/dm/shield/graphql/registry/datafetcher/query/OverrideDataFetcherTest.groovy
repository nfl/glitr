package com.nfl.dm.shield.graphql.registry.datafetcher.query

import graphql.Scalars
import graphql.schema.DataFetchingEnvironment
import spock.lang.Specification

class OverrideDataFetcherTest extends Specification {

    def env = new DataFetchingEnvironment(new DummyClass(), null, null, null, Scalars.GraphQLString, null, null);

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
