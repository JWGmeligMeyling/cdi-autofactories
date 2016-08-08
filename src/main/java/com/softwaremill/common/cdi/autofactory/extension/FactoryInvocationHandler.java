package com.softwaremill.common.cdi.autofactory.extension;

import com.softwaremill.common.cdi.autofactory.extension.parameter.ParameterValue;
import org.jboss.weld.Container;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedConstructor;
import org.jboss.weld.annotated.enhanced.jlr.EnhancedAnnotatedConstructorImpl;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedConstructor;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.injection.ConstructorInjectionPoint;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.ThreadLocalStack.ThreadLocalStackReference;
import org.jboss.weld.manager.BeanManagerImpl;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class FactoryInvocationHandler<T> implements InvocationHandler {
    private final BeanManager beanManager;
    private final Bean<T> bean;
    private final CreatedTypeData<T> createdTypeData;
    private final CreationalContext<T> creationalContext;

    public FactoryInvocationHandler(BeanManager beanManager, Bean<T> bean, CreatedTypeData<T> createdTypeData,
                                    CreationalContext<T> creationalContext) {
        this.beanManager = beanManager;
        this.bean = bean;
        this.createdTypeData = createdTypeData;
        this.creationalContext = creationalContext;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] parameters) throws Throwable {
        T instance;

        if (createdTypeData.isConstructorInjection()) {
            CurrentInjectionPoint currentInjectionPoint = Container.instance().services().get(CurrentInjectionPoint.class);
            Class<?> declaringComponentClass = (Class<T>) createdTypeData.getCreatedTypeConstructor().getBaseType();


            BeanManagerImpl manager = ((BeanManagerProxy) beanManager).delegate();
            EnhancedAnnotatedConstructor<T> constructor = (EnhancedAnnotatedConstructor<T>) manager
                .createEnhancedAnnotatedType(declaringComponentClass)
                .getEnhancedConstructors()
                .stream().findAny().get();

            ConstructorInjectionPoint<T> actualInjectionPoint = InjectionPointFactory.instance()
                .createConstructorInjectionPoint(bean, declaringComponentClass, constructor, manager);
            ThreadLocalStackReference<InjectionPoint> ref = currentInjectionPoint.push(actualInjectionPoint);
            instance = newInstance(parameters);
            ref.pop();
        } else {
            instance = newInstance(parameters);
            createdTypeData.getCreatedTypeInjectionTarget().inject(instance, creationalContext);
            createdTypeData.getCreatedTypeInjectionTarget().postConstruct(instance);
        }

        return instance;
    }

    private T newInstance(Object[] parameters) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return createdTypeData.getCreatedTypeConstructor().getJavaMember().newInstance(createConstructorParameters(parameters));
    }

    private Object[] createConstructorParameters(Object[] factoryMethodParameters) {
        ParameterValue[] createdTypeConstructorParameterValues = createdTypeData.getCreatedTypeConstructorParameterValues();

        Object[] constructorParameters =  new Object[createdTypeConstructorParameterValues.length];

        for (int i = 0; i < createdTypeConstructorParameterValues.length; i++) {
            ParameterValue createdTypeConstructorParameterValue = createdTypeConstructorParameterValues[i];
            constructorParameters[i] = createdTypeConstructorParameterValue.getValue(beanManager, factoryMethodParameters);
        }

        return constructorParameters;
    }
}
