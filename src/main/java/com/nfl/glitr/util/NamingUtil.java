package com.nfl.glitr.util;

import javax.validation.constraints.NotNull;

/**
 * Utility for providing names acceptable by graphql.
 */
public class NamingUtil {

    /**
     * Creates a graphql-compatible class name for the given class. Graphql doesn't accept
     * dots as valid characters, so those are substituted by underscores. For example, it will
     * make <tt>java.lang.Object</tt> into <tt>java_lang_Object</tt>.
     *
     * When naming subtypes, dollar signs will be replaced by double underscore characters, so
     * <tt>com.nfl.glitr.registry.type.GraphQLEnumTypeFactoryTest$ProfileType</tt> becomes
     * <tt>com_nfl_glitr_registry_type_GraphQLEnumTypeFactoryTest__ProfileType</tt>
     *
     * @param clazz the class to transform
     * @return a transformed class name
     */
    public static String compatibleClassName(@NotNull Class clazz) {
        return clazz.getName().replaceAll("\\.", "_").replaceAll("\\$", "__");
    }
}
