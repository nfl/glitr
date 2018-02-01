package com.nfl.glitr.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeUtil {

    public static final String PATH_SEPARATOR = "->";


    public static String buildNewPath(String parent, String... child) {
        return Optional.ofNullable(parent)
                .map(p -> Stream.concat(Stream.of(p),Stream.of(child)).collect(Collectors.joining(PATH_SEPARATOR)))
                .orElse(StringUtils.join(child, PATH_SEPARATOR));
    }
}
