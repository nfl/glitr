package com.nfl.glitr.data.query;

import com.nfl.glitr.annotation.GlitrQueryComplexity;

public interface Playable extends Identifiable {

    @GlitrQueryComplexity("5")
    String getUrl();
}
