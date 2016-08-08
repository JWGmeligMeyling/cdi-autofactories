package com.softwaremill.common.cdi.autofactory.extension.parameter;

import org.jboss.weld.Container;
import org.jboss.weld.injection.ConstructorInjectionPoint;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.ParameterInjectionPoint;
import org.jboss.weld.injection.ThreadLocalStack;
import org.jboss.weld.injection.ThreadLocalStack.ThreadLocalStackReference;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BeanManagerParameterValue implements ParameterValue {
    private final Type beanType;
    private final Annotation[] qualifiers;

    public BeanManagerParameterValue(Type beanType, Annotation[] qualifiers) {
        this.beanType = beanType;
        this.qualifiers = qualifiers;
    }

    @Override
    public Object getValue(BeanManager bm, Object[] factoryParameters) {
        CurrentInjectionPoint currentInjectionPointStack = Container.instance().services().get(CurrentInjectionPoint.class);
        ConstructorInjectionPoint currentInjectionPoint = (ConstructorInjectionPoint) currentInjectionPointStack.peek();
        ThreadLocalStackReference<?> ref = currentInjectionPointStack.push(findParameterInjectionPoint(currentInjectionPoint));
        Object result = lookup(bm, beanType, qualifiers);
        ref.pop();
        return result;
    }

    private ParameterInjectionPoint findParameterInjectionPoint(ConstructorInjectionPoint constructorInjectionPoint) {
        for (Object parameterInjectionPointObj : constructorInjectionPoint.getParameterInjectionPoints()) {
            ParameterInjectionPoint parameterInjectionPoint = (ParameterInjectionPoint) parameterInjectionPointObj;
            if (beanType.equals(parameterInjectionPoint.getType())) {
                return parameterInjectionPoint;
            }
        }

        throw new RuntimeException("Cannot find a parameter injection point for " + beanType + " in constructor " +
                constructorInjectionPoint);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T lookup(BeanManager manager, Class<T> beanClass, Annotation... qualifiers) {
        return (T) lookup(manager, (Type) beanClass, qualifiers);
    }


    @SuppressWarnings({"unchecked"})
    public static Object lookup(BeanManager manager, Type beanType, Annotation... qualifiers) {
        Set<?> beans = manager.getBeans(beanType, qualifiers);
        if (beans.size() != 1) {
            if (beans.size() == 0) {
                throw new RuntimeException("No beans of class " + beanType + " found.");
            } else {
                throw new RuntimeException("Multiple beans of class " + beanType + " found: " + beans + ".");
            }
        }

        Bean myBean = (Bean) beans.iterator().next();

        return manager.getReference(myBean, beanType, manager.createCreationalContext(myBean));
    }

}
