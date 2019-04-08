package com.nfl.glitr.registry.schema;

import graphql.language.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlitrFieldDefinition extends FieldDefinition {

    private Set<GlitrMetaDefinition> metaDefinitions = new HashSet<>();


    public GlitrFieldDefinition(String name) {
        this(name, new TypeName(name));
    }

    public GlitrFieldDefinition(String name, Set<GlitrMetaDefinition> metaDefinitions) {
        this(name);
        this.metaDefinitions = metaDefinitions;
    }

    public GlitrFieldDefinition(String name, Type type) {
        super(name, type);
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
