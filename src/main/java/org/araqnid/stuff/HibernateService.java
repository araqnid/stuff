package org.araqnid.stuff;

import org.araqnid.stuff.services.ProviderService;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import com.google.inject.Inject;

public class HibernateService extends ProviderService<SessionFactory> {
	@Inject
	public HibernateService() {
		super(SessionFactory.class);
	}

	private SessionFactory sessionFactory;

	@Override
	protected void startUp() throws Exception {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		StandardServiceRegistry serviceRegistry = registryBuilder.build();
		Configuration cfg = new Configuration();
		sessionFactory = cfg.buildSessionFactory(serviceRegistry);
	}

	@Override
	protected void shutDown() throws Exception {
		sessionFactory.close();
	}

	@Override
	protected SessionFactory getValue() {
		return sessionFactory;
	}
}
