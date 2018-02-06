package com.nfl.glitr.registry.schema;

import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlitrFieldDefinition extends FieldDefinition {

    private Set<GlitrMetaDefinition> metaDefinitions = new HashSet<>();


    public GlitrFieldDefinition(String name) {
        super(name);
    }

    public GlitrFieldDefinition(String name, Set<GlitrMetaDefinition> metaDefinitions) {
        super(name);
        this.metaDefinitions = metaDefinitions;
    }

    public GlitrFieldDefinition(String name, Type type) {
        super(name, type);
    }

    public GlitrFieldDefinition(String name, Type type, List<InputValueDefinition> inputValueDefinitions, List<Directive> directives) {
        super(name, type, inputValueDefinitions, directives);
    }

    public GlitrFieldDefinition(String name, Type type, List<InputValueDefinition> inputValueDefinitions, List<Directive> directives, Set<GlitrMetaDefinition> metaDefinitions) {
        super(name, type, inputValueDefinitions, directives);
        this.metaDefinitions = metaDefinitions;
    }

    public Set<GlitrMetaDefinition> getMetaDefinitions() {
        return metaDefinitions;
    }

    public void setMetaDefinitions(Set<GlitrMetaDefinition> metaDefinitions) {
        this.metaDefinitions = metaDefinitions;
    }

    public void addMetaDefinitions(GlitrMetaDefinition metaDefinition) {
        if (this.metaDefinitions == null) {
            this.metaDefinitions = new HashSet<>();
        }
        this.metaDefinitions.add(metaDefinition);
    }
}
