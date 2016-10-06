package com.nfl.glitr.registry.datafetcher.query;

import com.nfl.glitr.exception.GlitrException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OverrideDataFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(OverrideDataFetcher.class);

    private Object override;
    private Method overrideMethod;


    /**
     * This constructor is used for getter overrides placed in the class itself
     *
     * @param name  field name
     * @param clazz schema class
     */
    public OverrideDataFetcher(String name, Class clazz) {
        overrideMethod = findMethod("get" + StringUtils.capitalize(name), clazz);
        if (overrideMethod != null) {
            return;
        }
        overrideMethod = findMethod("is" + StringUtils.capitalize(name), clazz);
    }

    /**
     * This constructor is used for getter overrides outside of the reference class
     *
     * @param name     field name
     * @param override override object
     */
    public OverrideDataFetcher(String name, Object override) {
        this(name, override.getClass());
        this.override = override;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (overrideMethod == null) {
            return null;
        }

        Object obj = override == null ? environment.getSource() : override;

        try {
            return overrideMethod.invoke(obj, environment);
        } catch (InvocationTargetException e) {
            logger.error("Something went wrong - Unable to fetch result for overrideMethod={{}} obj={} and environment={}", overrideMethod, obj, environment, e);
            // If the override method threw a RuntimeException just send it up
            if(e.getTargetException() instanceof RuntimeException)
                throw (RuntimeException) e.getTargetException();
            // Otherwise, wrap it up in a Glitr Exception
            throw new GlitrException("Overwrite method exception", e.getTargetException());
        } catch (Exception e) {
            logger.error("Something went wrong - Unable to fetch result for overrideMethod={{}} obj={} and environment={}", overrideMethod, obj, environment, e);
        }
        return null;
    }

    private Method findMethod(String name, Class clazz) {
        try {
            //noinspection unchecked
            return clazz.getMethod(name, DataFetchingEnvironment.class);
        } catch (NoSuchMethodException e) {
            logger.debug("Couldn't find method for name {} and class {}", name, clazz, e.getMessage());
        }
        return null;
    }

    public Method getOverrideMethod() {
        return overrideMethod;
    }

    public Object getOverride() {
        return override;
    }
}
