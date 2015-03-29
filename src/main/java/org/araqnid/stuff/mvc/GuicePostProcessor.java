package org.araqnid.stuff.mvc;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.google.inject.Injector;

public class GuicePostProcessor implements BeanPostProcessor {
	private final Injector injector;

	@Autowired
	public GuicePostProcessor(ServletContext servletContext) {
		injector = (Injector) servletContext.getAttribute(Injector.class.getName());
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		injector.injectMembers(bean);
		return bean;
	}
}
