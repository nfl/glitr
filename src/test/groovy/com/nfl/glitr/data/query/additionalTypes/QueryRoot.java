package com.nfl.glitr.data.query.additionalTypes;

import com.nfl.glitr.annotation.GlitrArgument;
import com.nfl.glitr.data.query.PublishType;
import com.nfl.glitr.data.query.Video;
import java.util.List;

public class QueryRoot {

    public Person getPerson() {
        return null;
    }

    @GlitrArgument(name = "video_default", defaultValue = "No Default Value", type = String.class, nullable = false)
    public List<Video> getDefaultVideo() {
        return null;
    }

    @GlitrArgument(name = "video_non_enum", defaultValue = "defaultTest", type = String.class, nullable = false)
    public List<Video> getNonEnumVideo() {
        return null;
    }

    @GlitrArgument(name = "video_enum", defaultValue = "PUBLISHED", type = PublishType.class, nullable = false)
    public List<Video> getEnumVideo() {
        return null;
    }

    @GlitrArgument(name = "video_enum_fail", defaultValue = "FAKE_ENUM", type = PublishType.class, nullable = false)
    public List<Video> getEnumFailVideo() {
        return null;
    }
}