package com.nfl.glitr.data.query.additionalTypes;

import com.nfl.glitr.annotation.GlitrArgument;
import com.nfl.glitr.data.query.PublishType;
import com.nfl.glitr.data.query.Video;

import java.util.List;

import static com.nfl.glitr.annotation.GlitrArgument.nullability.NON_BLANK;
import static com.nfl.glitr.annotation.GlitrArgument.nullability.NON_NULL;

public class QueryRoot {

    public Person getPerson() {
        return null;
    }

    @GlitrArgument(name = "video_default", defaultValue = "No Default Value", type = String.class, nullability = NON_BLANK)
    public List<Video> getDefaultVideo() {
        return null;
    }

    @GlitrArgument(name = "video_non_enum", defaultValue = "defaultTest", type = String.class, nullability = NON_NULL)
    public List<Video> getNonEnumVideo() {
        return null;
    }

    @GlitrArgument(name = "video_enum", defaultValue = "PUBLISHED", type = PublishType.class, nullability = NON_NULL)
    public List<Video> getEnumVideo() {
        return null;
    }

    @GlitrArgument(name = "video_enum_fail", defaultValue = "FAKE_ENUM", type = PublishType.class, nullability = NON_NULL)
    public List<Video> getEnumFailVideo() {
        return null;
    }
}