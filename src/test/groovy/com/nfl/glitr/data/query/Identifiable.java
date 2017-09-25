package com.nfl.glitr.data.query;

import com.nfl.glitr.annotation.GlitrDescription;

@GlitrDescription("Identifiable interface needed for Relay")
public interface Identifiable {

    String getId();
}
