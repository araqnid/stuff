package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector;
import com.fasterxml.jackson.module.guice.GuiceInjectableValues;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import com.google.inject.util.Providers;

public class XmlMapperModule extends AbstractModule {
	private final Set<Module> prebuiltModules = new HashSet<>();
	private final Set<Key<? extends Module>> referencedModules = new HashSet<>();
	private com.google.inject.Scope scope = Scopes.NO_SCOPE;
	private Class<? extends Annotation> scopeAnnotationClass;
	private final Key<XmlMapper> key;
	private boolean enableJaxbIntrospection;

	public XmlMapperModule() {
		key = Key.get(XmlMapper.class);
	}

	@SuppressWarnings("restriction")
	public XmlMapperModule(Class<? extends Annotation> bindingAnnotation) {
		Preconditions.checkArgument(
				bindingAnnotation.getAnnotation(Qualifier.class) != null
						|| bindingAnnotation.getAnnotation(com.google.inject.BindingAnnotation.class) != null,
				"scopeAnnotation must bear the @Qualifier (or @BindingAnnotation) meta-annotation");
		key = Key.get(XmlMapper.class, bindingAnnotation);
	}

	@SuppressWarnings("restriction")
	public XmlMapperModule(Annotation bindingAnnotation) {
		Preconditions.checkArgument(bindingAnnotation.getClass().getAnnotation(Qualifier.class) != null
				|| bindingAnnotation.getClass().getAnnotation(com.google.inject.BindingAnnotation.class) != null,
				"scopeAnnotation must bear the @Qualifier (or @BindingAnnotation) meta-annotation");
		key = Key.get(XmlMapper.class, bindingAnnotation);
	}

	@Override
	protected void configure() {
		Set<Provider<? extends Module>> providers = new HashSet<>();
		Set<Dependency<?>> dependencies = new HashSet<>();
		for (Key<? extends Module> moduleKey : referencedModules) {
			providers.add(binder().getProvider(moduleKey));
			dependencies.add(Dependency.get(moduleKey));
		}
		for (Module module : prebuiltModules) {
			binder().requestInjection(module);
			providers.add(Providers.of(module));
		}
		MapperProvider mapperProvider = new MapperProvider(providers, dependencies, enableJaxbIntrospection);
		if (scope != null) {
			bind(key).toProvider(mapperProvider).in(scope);
		} else {
			bind(key).toProvider(mapperProvider).in(scopeAnnotationClass);
		}
	}

	private static final class MapperProvider implements ProviderWithDependencies<XmlMapper> {
		private final Set<Provider<? extends Module>> providers;
		private final Set<Dependency<?>> dependencies;
		private final boolean enableJaxbIntrospection;
		@Inject
		private Injector injector;

		private MapperProvider(Set<Provider<? extends Module>> providers, Set<Dependency<?>> dependencies,
				boolean enableJaxbIntrospection) {
			this.providers = ImmutableSet.copyOf(providers);
			this.dependencies = ImmutableSet.copyOf(dependencies);
			this.enableJaxbIntrospection = enableJaxbIntrospection;
		}

		@Override
		public XmlMapper get() {
			XmlMapper mapper = new XmlMapper();
			for (Provider<? extends Module> provider : providers) {
				mapper.registerModule(provider.get());
			}
			mapper.setInjectableValues(new GuiceInjectableValues(injector));
			addGuiceIntrospector(mapper);
			if (enableJaxbIntrospection) {
				addJaxbIntrospector(mapper);
			}
			return mapper;
		}

		private void addGuiceIntrospector(XmlMapper mapper) {
			GuiceAnnotationIntrospector guiceIntrospector = new GuiceAnnotationIntrospector();
			AnnotationIntrospectorPair ser = new AnnotationIntrospectorPair(guiceIntrospector, mapper
					.getSerializationConfig().getAnnotationIntrospector());
			AnnotationIntrospectorPair deser = new AnnotationIntrospectorPair(guiceIntrospector, mapper
					.getDeserializationConfig().getAnnotationIntrospector());
			mapper.setAnnotationIntrospectors(ser, deser);
		}

		private void addJaxbIntrospector(XmlMapper mapper) {
			JaxbAnnotationIntrospector jaxbIntrospector = new JaxbAnnotationIntrospector(mapper.getTypeFactory());
			AnnotationIntrospectorPair ser = new AnnotationIntrospectorPair(jaxbIntrospector, mapper
					.getSerializationConfig().getAnnotationIntrospector());
			AnnotationIntrospectorPair deser = new AnnotationIntrospectorPair(jaxbIntrospector, mapper
					.getDeserializationConfig().getAnnotationIntrospector());
			mapper.setAnnotationIntrospectors(ser, deser);
		}

		@Override
		public Set<Dependency<?>> getDependencies() {
			return dependencies;
		}
	}

	public XmlMapperModule registerModule(Class<? extends Module> moduleClass) {
		return registerModule(Key.get(moduleClass));
	}

	public XmlMapperModule registerModule(TypeLiteral<? extends Module> moduleClass) {
		return registerModule(Key.get(moduleClass));
	}

	public XmlMapperModule registerModule(Key<? extends Module> moduleKey) {
		referencedModules.add(moduleKey);
		return this;
	}

	public XmlMapperModule registerModule(Module instance) {
		prebuiltModules.add(instance);
		return this;
	}

	public XmlMapperModule in(com.google.inject.Scope scopeInstance) {
		this.scope = scopeInstance;
		this.scopeAnnotationClass = null;
		return this;
	}

	public XmlMapperModule usingJaxbAnnotations() {
		this.enableJaxbIntrospection = true;
		return this;
	}

	@SuppressWarnings("restriction")
	public XmlMapperModule in(Class<? extends Annotation> ann) {
		Preconditions.checkArgument(
				ann.getAnnotation(Scope.class) != null
						|| ann.getAnnotation(com.google.inject.ScopeAnnotation.class) != null,
				"scopeAnnotation must bear the @Scope (or @ScopeAnnontation) meta-annotation");
		this.scopeAnnotationClass = ann;
		return this;
	}
}
