package com.nfl.glitr.data.query;

import com.nfl.glitr.annotation.GlitrForwardPagingArguments;
import com.nfl.glitr.annotation.GlitrQueryComplexity;
import com.nfl.glitr.data.mutation.Bitrate;

import java.util.List;

public class Video extends AbstractContent implements Playable {

    private String id;
    private String url;
    private List<Bitrate> bitrateList;


    public List<Bitrate> getBitrateList() {
        return bitrateList;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    @GlitrForwardPagingArguments
    public List<Video> getChildren() {
        return null;
    }

    @GlitrQueryComplexity("#{depth}")
    public List<Video> getDepth() {
        return null;
    }

    @GlitrQueryComplexity("#{totalCollectionsSize}")
    public List<Video> getTotalCollectionsSize() {
        return null;
    }

    @GlitrQueryComplexity("#{depth} + #{childScore} + #{currentCollectionSize} + #{totalCollectionsSize} + #{maxCharacterLimit} + #{maxDepthLimit} + #{maxScoreLimit} + #{defaultMultiplier} + 5")
    public List<Video> getAllVariablesComplexityFormula() {
        return null;
    }
}
