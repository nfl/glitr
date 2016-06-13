package com.nfl.dm.shield.util

import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import graphql.schema.GraphQLObjectType

import static graphql.Scalars.GraphQLInt
import static graphql.Scalars.GraphQLString
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition

class GraphQLTestUtils {
    public static Field getRootFieldFromQuery(String query) {
        Parser parser = new Parser();
        def document = parser.parseDocument(query)


        def definition = (OperationDefinition) document.getDefinitions().get(0)
        def viewerField = (Field) definition.getSelectionSet().getSelections().get(0)
        def rootField = viewerField.selectionSet.selections.get(0);

        return rootField
    }

    public static Field getFieldFromQuery(String query, String paths) {
        Field field = getRootFieldFromQuery(query);

        def result = field;

        for (String path : paths.split("\\.")) {
            def selections = result.getSelectionSet().selections

            result = selections.find { it instanceof Field && it.name.equals(path)}
        }

        return result;
    }


    public static GraphQLObjectType newDummyGraphQLObjectType() {
        return GraphQLObjectType.newObject()
                .field(newFieldDefinition()
                .name("dummyField")
                .type(GraphQLInt)
                .build())
                .name("dummy")
                .build();
    }

    public static GraphQLObjectType newDummyGraphQLRelayConnectionObjectType() {
        return GraphQLObjectType.newObject()
                .field(newFieldDefinition()
                .name("edges")
                .type(GraphQLString)
                .build())
                .name("dummy")
                .build();
    }
}
