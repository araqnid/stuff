package org.araqnid.stuff.config;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.ScopeAnnotation;
import com.google.inject.Singleton;
import com.google.inject.Stage;

public class CrossScopeTest {
	private Map<Key<?>, Object> scopedThings;
	private final Module testScopeModule = new AbstractModule() {
		@Override
		protected void configure() {
			bindScope(TestScope.class, new Scope() {
				@Override
				public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
					return new Provider<T>() {
						@Override
						public T get() {
							if (scopedThings == null) throw new IllegalStateException("Scope not entered yet");
							if (!scopedThings.containsKey(key)) {
								scopedThings.put(key, unscoped.get());
							}
							@SuppressWarnings("unchecked")
							T thing = (T) scopedThings.get(key);
							return thing;
						}
					};
				}
			});
			bind(SomeScopedThing.class);
		}
	};

	@Test
	public void scoped_object_returned_from_map() {
		Injector injector = Guice.createInjector(testScopeModule);
		SomeScopedThing instance1 = new SomeScopedThing();
		scopedThings = ImmutableMap.<Key<?>, Object> of(Key.get(SomeScopedThing.class), instance1);
		SomeScopedThing instance2 = injector.getInstance(SomeScopedThing.class);
		assertSame(instance1, instance2);
	}

	@Test
	public void scoped_object_created_in_scope_and_reused() {
		Injector injector = Guice.createInjector(testScopeModule);
		scopedThings = Maps.newHashMap();
		SomeScopedThing instance1 = injector.getInstance(SomeScopedThing.class);
		SomeScopedThing instance2 = injector.getInstance(SomeScopedThing.class);
		assertSame(instance1, instance2);
	}

	@Test
	public void clearing_scope_map_causes_creation_of_a_new_instance() {
		Injector injector = Guice.createInjector(testScopeModule);
		scopedThings = Maps.newHashMap();
		SomeScopedThing instance1 = injector.getInstance(SomeScopedThing.class);
		scopedThings.clear();
		SomeScopedThing instance2 = injector.getInstance(SomeScopedThing.class);
		assertNotSame(instance1, instance2);
	}

	@Test
	public void good_singleton_reused_after_clearing_scope() {
		Injector injector = Guice.createInjector(testScopeModule, new AbstractModule() {
			@Override
			protected void configure() {
				bind(SingletonThing.class).to(GoodSingletonThing.class);
			}
		});
		scopedThings = Maps.newHashMap();
		SingletonThing instance1 = injector.getInstance(SingletonThing.class);
		scopedThings.clear();
		SingletonThing instance2 = injector.getInstance(SingletonThing.class);
		assertSame(instance1, instance2);
	}

	@Test
	public void good_singleton_returns_a_new_scoped_thing_after_clearing_scope() {
		Injector injector = Guice.createInjector(testScopeModule, new AbstractModule() {
			@Override
			protected void configure() {
				bind(SingletonThing.class).to(GoodSingletonThing.class);
			}
		});
		scopedThings = Maps.newHashMap();
		SomeScopedThing instance1scoped = injector.getInstance(SingletonThing.class).ourScopedThing();
		scopedThings.clear();
		SomeScopedThing instance2scoped = injector.getInstance(SingletonThing.class).ourScopedThing();
		assertNotSame(instance1scoped, instance2scoped);
	}

	@Test
	public void evil_singleton_does_not_return_a_new_scoped_thing_after_clearing_scope() {
		Injector injector = Guice.createInjector(testScopeModule, new AbstractModule() {
			@Override
			protected void configure() {
				bind(SingletonThing.class).to(EvilSingletonThing.class);
			}
		});
		scopedThings = Maps.newHashMap();
		SomeScopedThing instance1scoped = injector.getInstance(SingletonThing.class).ourScopedThing();
		scopedThings.clear();
		SomeScopedThing instance2scoped = injector.getInstance(SingletonThing.class).ourScopedThing();
		assertSame(instance1scoped, instance2scoped);
	}

	@Test(expected = CreationException.class)
	public void evil_singleton_trapped_early_if_injector_created_as_production_stage() {
		Guice.createInjector(Stage.PRODUCTION, testScopeModule, new AbstractModule() {
			@Override
			protected void configure() {
				bind(SingletonThing.class).to(EvilSingletonThing.class);
			}
		});
	}

	@ScopeAnnotation
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TestScope {
	}

	@TestScope
	public static class SomeScopedThing {
	}

	public interface SingletonThing {
		SomeScopedThing ourScopedThing();
	}

	@Singleton
	public static class EvilSingletonThing implements SingletonThing {
		private final SomeScopedThing scopedThing;

		@Inject
		public EvilSingletonThing(SomeScopedThing scopedThing) {
			this.scopedThing = scopedThing;
		}

		@Override
		public SomeScopedThing ourScopedThing() {
			return scopedThing;
		}
	}

	@Singleton
	public static class GoodSingletonThing implements SingletonThing {
		private final Provider<SomeScopedThing> scopedThingProvider;

		@Inject
		public GoodSingletonThing(Provider<SomeScopedThing> scopedThingProvider) {
			this.scopedThingProvider = scopedThingProvider;
		}

		@Override
		public SomeScopedThing ourScopedThing() {
			return scopedThingProvider.get();
		}
	}
}
