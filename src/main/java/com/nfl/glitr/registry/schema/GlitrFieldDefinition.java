package com.nfl.glitr.registry.schema;

import graphql.language.FieldDefinition;
import graphql.language.Type;

import java.util.HashSet;
import java.util.Set;

public class GlitrFieldDefinition extends FieldDefinition {

    private Set<GlitrMetaDefinition> metaDefinitions = new HashSet<>();


    public GlitrFieldDefinition(String name, Type type) {
        super(name, type);
    }

    public GlitrFieldDefinition(String name, Set<GlitrMetaDefinition> metaDefinitions) {
        super(name, null);
        this.metaDefinitions = metaDefinitions;
    }

    public GlitrFieldDefinition(String name, Type type, Set<GlitrMetaDefinition> metaDefinitions) {
        super(name, type);
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
