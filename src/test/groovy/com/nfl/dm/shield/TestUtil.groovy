package com.nfl.dm.shield

import org.springframework.aop.framework.Advised
import org.springframework.aop.support.AopUtils

import java.lang.reflect.Field

class TestUtil {


    /**
     * Set a field on an instance when there is no getter available.
     * Usually used to inject a mock inside of a Bean
     * @param fieldValue
     * @param fieldName
     * @param instance
     * @return
     */
    static def setFieldValueOnInstanceUsingReflection(Object fieldValue, String fieldName, Object instance) {
        def service = unwrapProxy(instance)
        Field field = service.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(instance, fieldValue)
    }

    /**
     * Unwrap proxy allows us to use mocks in classes with @Transcational
     * see http://kim.saabye-pedersen.org/2012/12/mockito-and-spring-proxies.html for more details
     * @param bean proxy to unwrap
     * @return bean
     * @throws Exception
     */
    static def unwrapProxy(Object bean) throws Exception {
        /*
        * If the given object is a proxy, set the return value as the object
        * being proxied, otherwise return the given object.
        */
        if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
            Advised advised = (Advised) bean;
            bean = advised.getTargetSource().getTarget();
        }
        return bean;
    }
}
