package com.nfl.glitr.util;

import java.util.Optional;

public class NodeUtil {
    public static final String PATH_SEPARATOR = "->";

    public static String buildPath(String parent, String child) {
        return Optional.ofNullable(parent)
                .map(p -> p + PATH_SEPARATOR + child)
                .orElse(child);
    }
}
