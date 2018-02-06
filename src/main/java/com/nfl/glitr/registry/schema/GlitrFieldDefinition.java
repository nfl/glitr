package com.nfl.glitr.registry.schema;

import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.Type;

import java.util.ArrayList;
import java.util.List;

public class GlitrFieldDefinition extends FieldDefinition {

    private List<GlitrMetaDefinition> metaDefinitions = new ArrayList<>();


    public GlitrFieldDefinition(String name) {
        super(name);
    }

    public GlitrFieldDefinition(String name, List<GlitrMetaDefinition> metaDefinitions) {
        super(name);
        this.metaDefinitions = metaDefinitions;
    }

    public GlitrFieldDefinition(String name, Type type) {
        super(name, type);
    }

    public GlitrFieldDefinition(String name, Type type, List<InputValueDefinition> inputValueDefinitions, List<Directive> directives) {
        super(name, type, inputValueDefinitions, directives);
    }

    public GlitrFieldDefinition(String name, Type type, List<InputValueDefinition> inputValueDefinitions, List<Directive> directives, List<GlitrMetaDefinition> metaDefinitions) {
        super(name, type, inputValueDefinitions, directives);
        this.metaDefinitions = metaDefinitions;
    }

    public List<GlitrMetaDefinition> getMetaDefinitions() {
        return metaDefinitions;
    }

    public void setMetaDefinitions(List<GlitrMetaDefinition> metaDefinitions) {
        this.metaDefinitions = metaDefinitions;
    }

    public void addMetaDefinitions(GlitrMetaDefinition metaDefinition) {
        if (this.metaDefinitions == null) {
            this.metaDefinitions = new ArrayList<>();
        }
        this.metaDefinitions.add(metaDefinition);
    }
}
