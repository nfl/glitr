package com.nfl.glitr.registry.datafetcher.query

import com.nfl.glitr.exception.GlitrException
import com.nfl.glitr.exception.GlitrOverrideException
import graphql.Scalars
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import org.slf4j.Logger
import org.slf4j.event.Level
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class OverrideDataFetcherTest extends Specification {
    def execCtx = ExecutionContextBuilder.newExecutionContextBuilder()
            .executionId(ExecutionId.generate())
            .build();
    def env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(execCtx)
            .source(new DummyClass())
            .fieldType(Scalars.GraphQLString)
            .build()

    def loggerMock = Mock(Logger)

    def setup() {
        Field field = OverrideDataFetcher.getDeclaredField("logger")
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, loggerMock)
    }

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

    @Unroll("override method inside DummyClass, with #levelLog level error")
    def "override method inside DummyClass, with error"() {
        setup:
            def obj = [getError: { DataFetchingEnvironment environment -> throw new GlitrOverrideException(new RuntimeException(), levelLog) }] as Dummy
            def env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(execCtx)
                    .source(obj)
                    .fieldType(Scalars.GraphQLString)
                    .build()
            def overrideDataFetcher = new OverrideDataFetcher("error", obj.class)

        when:
            overrideDataFetcher.get(env)

        then:
            if (levelLog == Level.INFO) {
                1 * loggerMock.info(_, _ as Object[])
                0 * loggerMock.warn(_, _ as Object[])
                0 * loggerMock.error(_, _ as Object[])
                0 * loggerMock.debug(_, _ as Object[])
                0 * loggerMock.trace(_, _ as Object[])
            }

            if (levelLog == Level.WARN) {
                0 * loggerMock.info(_, _ as Object[])
                1 * loggerMock.warn(_, _ as Object[])
                0 * loggerMock.error(_, _ as Object[])
                0 * loggerMock.debug(_, _ as Object[])
                0 * loggerMock.trace(_, _ as Object[])
            }
            if (levelLog == Level.ERROR) {
                0 * loggerMock.info(_, _ as Object[])
                0 * loggerMock.warn(_, _ as Object[])
                1 * loggerMock.error(_, _ as Object[])
                0 * loggerMock.debug(_, _ as Object[])
                0 * loggerMock.trace(_, _ as Object[])
            }
            if (levelLog == Level.DEBUG) {
                0 * loggerMock.info(_, _ as Object[])
                0 * loggerMock.warn(_, _ as Object[])
                0 * loggerMock.error(_, _ as Object[])
                1 * loggerMock.debug(_, _ as Object[])
                0 * loggerMock.trace(_, _ as Object[])
            }
            if (levelLog == Level.TRACE) {
                0 * loggerMock.info(_, _ as Object[])
                0 * loggerMock.warn(_, _ as Object[])
                0 * loggerMock.error(_, _ as Object[])
                0 * loggerMock.debug(_, _ as Object[])
                1 * loggerMock.trace(_, _ as Object[])
            }
            if (levelLog == null)
                1 * loggerMock.error(_, _ as Object[])

            thrown(RuntimeException)

        where:
            levelLog << [Level.INFO, Level.WARN, Level.ERROR, Level.DEBUG, Level.TRACE, null]
    }

    @Unroll("override method inside DummyClass, with expected #expectedError error")
    def "override method inside DummyClass, with different kind of errors"() {
        setup:
            def obj = [getError: { DataFetchingEnvironment environment -> throw throwError }] as Dummy
            def env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(execCtx)
                    .source(obj)
                    .fieldType(Scalars.GraphQLString)
                    .build()
            def overrideDataFetcher = new OverrideDataFetcher("error", obj.class)

        when:
            overrideDataFetcher.get(env)

        then:
            1 * loggerMock.error(_, _ as Object[])
            0 * loggerMock.info(_, _ as Object[])
            0 * loggerMock.warn(_, _ as Object[])
            0 * loggerMock.debug(_, _ as Object[])
            0 * loggerMock.trace(_, _ as Object[])

            thrown(expectedError)

        where:
            throwError                   | expectedError
            new RuntimeException("test") | RuntimeException
            new IOException()            | GlitrException
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

    public interface Dummy {
        Object getError(DataFetchingEnvironment environment) throws IOException
    }
}
