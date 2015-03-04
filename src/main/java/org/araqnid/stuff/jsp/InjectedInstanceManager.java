package org.araqnid.stuff.jsp;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

import org.apache.tomcat.InstanceManager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

public class InjectedInstanceManager implements InstanceManager {
	private final Injector injector;
	@SuppressWarnings("rawtypes")
	private final Matcher<Class> classMatcher;

	@Inject
	public InjectedInstanceManager(Injector injector) {
		this.injector = injector;
		this.classMatcher = Matchers.inSubpackage("org.araqnid.stuff");
	}

	@Override
	public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException,
			NamingException, InstantiationException {
		return prepareInstance(clazz.newInstance());
	}

	@Override
	public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException,
			NamingException, InstantiationException, ClassNotFoundException {
		Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
		return prepareInstance(clazz.newInstance());
	}

	@Override
	public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException,
			InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
		Class<?> clazz = classLoader.loadClass(fqcn);
		return prepareInstance(clazz.newInstance());
	}

	@Override
	public void newInstance(Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
		prepareInstance(o);
	}

	@Override
	public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {
	}

	private Object prepareInstance(Object o) {
		if (classMatcher.matches(o.getClass())) {
			injector.injectMembers(o);
		}
		return o;
	}
}
