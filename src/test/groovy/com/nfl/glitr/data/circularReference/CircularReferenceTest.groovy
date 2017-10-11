package com.nfl.glitr.data.circularReference

import com.nfl.glitr.Glitr
import com.nfl.glitr.GlitrBuilder
import com.nfl.glitr.data.query.QueryType
import graphql.TypeResolutionEnvironment
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import spock.lang.Specification

import static com.nfl.glitr.util.NodeUtil.buildPath

class CircularReferenceTest extends Specification {

    def "Inspect AbstractClass that has a field circular reference"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
            GraphQLType type = glitr.typeRegistry.lookup(AbstractRead.class)
        then: "Make sure an interface is created"
            type instanceof GraphQLInterfaceType
            type.name == AbstractRead.simpleName
        then: "And make sure the implementing type has the interface registered"
            def objType = glitr.typeRegistry.getType(new TypeResolutionEnvironment(new Novel(),
                    null, null, null, null))
            objType.class == GraphQLObjectType
            objType.name == Novel.simpleName
            objType.fieldDefinitions.name as Set == ["novel", "pageCount", "title", "reviewed"] as Set
            objType.interfaces.name as Set == [AbstractRead.simpleName] as Set
        then: "And make sure there is no circular processing and complexity map populated only with one record where key is 'novel'"
            def queryComplexityMap = glitr.typeRegistry.getQueryComplexityMultipliersMap()
            queryComplexityMap.size() == 1
            queryComplexityMap.get("novel") == 12

    }

    def "Inspect Interface that has a field circular reference"() {
        setup:
            Glitr glitr = GlitrBuilder.newGlitr().withQueryRoot(new QueryType()).build()
        when:
            GraphQLType type = glitr.typeRegistry.lookup(Readable.class)
        then: "Make sure an interface is created"
            type instanceof GraphQLInterfaceType
            type.name == Readable.simpleName
        then: "And make sure the implementing type has the interface registered"
            def objType = glitr.typeRegistry.getType(new TypeResolutionEnvironment(new Book(),
                    null, null, null, null))
            objType.class == GraphQLObjectType
            objType.name == Book.simpleName
            objType.fieldDefinitions.name as Set == ["title", "synopsis"] as Set
            objType.interfaces.name as Set == [Readable.simpleName] as Set
        then: "And make sure there is no circular processing and complexity map populated only with one record where key is 'synopsis->synopsis'"
            def queryComplexityMap = glitr.typeRegistry.getQueryComplexityMultipliersMap()
            queryComplexityMap.size() == 1
            queryComplexityMap.get(buildPath("synopsis","synopsis")) == 12
    }
}
