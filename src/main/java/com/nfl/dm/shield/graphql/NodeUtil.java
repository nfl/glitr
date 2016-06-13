package com.nfl.dm.shield.graphql;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NodeUtil {

    private final static Logger logger = LoggerFactory.getLogger(NodeUtil.class);
    private Map<String, Boolean> fieldHasIdMap = new HashMap<>();
    public final static String NAME_PREFIX = "class ";


    public String getIdFromField(Object parent, String fieldName) {
        if (isNode(parent.getClass(), fieldName)) {
            try {
                return BeanUtils.getNestedProperty(parent, fieldName + "." + "id");
            } catch (Exception e) {
                logger.warn("Should not get here", e);
                return null;
            }
        }

        return null;
    }

    public boolean isNull(Object parent, String fieldName) {
        try {
            return BeanUtils.getProperty(parent, fieldName) == null;
        } catch (Exception e) {
            logger.warn("Should not get here", e);
            return true;
        }
    }

    private Boolean isNode(Class clazz, String fieldName) {
        String key = clazz.getName() + ":" + fieldName;
        if (fieldHasIdMap.get(key) != null) {
            return fieldHasIdMap.get(key);
        }

        Boolean isNode;
        PropertyDescriptor propertyDescriptor = Arrays.stream(PropertyUtils.getPropertyDescriptors(clazz)).filter(pd -> pd.getName().equals(fieldName)).findAny().get();

        //TODO: Improve this to support List types.
        Class<?> type = propertyDescriptor.getPropertyType();
        Optional<PropertyDescriptor> optional = Arrays.stream(PropertyUtils.getPropertyDescriptors(type)).filter(pd -> pd.getName().equals("id")).findAny();

        isNode = optional.isPresent();
        fieldHasIdMap.put(key, isNode);
        return isNode;
    }

    public static  String getClassName(Type type) {
        String fullName = type.toString();
        if (fullName.startsWith(NAME_PREFIX)) {
            return fullName.substring(NAME_PREFIX.length());
        }
        return fullName;
    }

    public static Class getClassFromType(Type type) {
        try {
            return Class.forName(getClassName(type));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
