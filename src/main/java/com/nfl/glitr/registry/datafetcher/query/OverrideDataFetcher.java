package com.nfl.glitr.registry.datafetcher.query;

import com.nfl.glitr.exception.GlitrException;
import com.nfl.glitr.exception.GlitrOverrideException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OverrideDataFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(OverrideDataFetcher.class);
    private static final String OVERRIDE_METHOD_ERROR_MESSAGE = "Something went wrong - Unable to fetch result for overrideMethod={{}} of {}";

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
            Throwable targetException = e.getTargetException();

            // If the override method threw a RuntimeException just send it up
            if (targetException instanceof RuntimeException) {
                log(overrideMethod.getName(), obj.getClass().getSimpleName(), targetException);
                throw (RuntimeException) targetException;
            }

            // Otherwise, wrap it up in a Glitr Exception
            log(overrideMethod.getName(), obj.getClass().getSimpleName(), targetException);
            throw new GlitrException("Overwrite method exception", targetException);
        } catch (Exception e) {
            log(overrideMethod.getName(), obj.getClass().getSimpleName(), e);
        }
        return null;
    }

    private void log(String methodName, String className, Throwable targetException) {
        Object[] args = new Object[targetException instanceof RuntimeException ? 3 : 2];
        args[0] = methodName;
        args[1] = className;
        if (targetException instanceof RuntimeException) {
            args[2] = targetException instanceof GlitrOverrideException ? targetException.getCause() : targetException;
        }

        if (targetException instanceof GlitrOverrideException && ((GlitrOverrideException) targetException).getLogLevel() != null) {
            switch (((GlitrOverrideException) targetException).getLogLevel()) {
                case INFO:
                    logger.info(OVERRIDE_METHOD_ERROR_MESSAGE, args);
                    break;
                case WARN:
                    logger.warn(OVERRIDE_METHOD_ERROR_MESSAGE, args);
                    break;
                case ERROR:
                    logger.error(OVERRIDE_METHOD_ERROR_MESSAGE, args);
                    break;
                case DEBUG:
                    logger.debug(OVERRIDE_METHOD_ERROR_MESSAGE, args);
                    break;
                case TRACE:
                    logger.trace(OVERRIDE_METHOD_ERROR_MESSAGE, args);
                    break;
                default:
                    logger.error(OVERRIDE_METHOD_ERROR_MESSAGE, args);
                    break;
            }
        } else {
            logger.error(OVERRIDE_METHOD_ERROR_MESSAGE, args);
        }
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
