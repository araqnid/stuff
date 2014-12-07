package org.araqnid.stuff.jsp;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.tomcat.InstanceManager;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.MembersInjector;

public class InjectedInstanceManager implements InstanceManager {
	private final Map<Class<?>, MembersInjector<?>> specificInjectors;

	@Inject
	public InjectedInstanceManager(MembersInjector<ServerIdentityTag> tagInjector) {
		this.specificInjectors = ImmutableMap.<Class<?>, MembersInjector<?>> of(ServerIdentityTag.class, tagInjector);
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

	@SuppressWarnings("unchecked")
	private Object prepareInstance(Object o) {
		MembersInjector<Object> specificInjector = (MembersInjector<Object>) specificInjectors.get(o.getClass());
		if (specificInjector != null) {
			specificInjector.injectMembers(o);
		}
		return o;
	}
}
