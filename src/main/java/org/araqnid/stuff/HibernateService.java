package org.araqnid.stuff;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.araqnid.stuff.services.ProviderService;
import org.araqnid.stuff.workqueue.WorkItem;
import org.araqnid.stuff.workqueue.WorkItemEvent;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import com.google.common.collect.ImmutableMap;

public class HibernateService extends ProviderService<SessionFactory> {
	private final DataSource dataSource;

	@Inject
	public HibernateService(DataSource dataSource) {
		super(SessionFactory.class);
		this.dataSource = dataSource;
	}

	private SessionFactory sessionFactory;

	@Override
	protected void startUp() throws Exception {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass(WorkItem.class);
		cfg.addAnnotatedClass(WorkItemEvent.class);
		cfg.setNamingStrategy(new ImprovedNamingStrategy());
		cfg.setProperty(Environment.USE_SQL_COMMENTS, "true");
		cfg.setProperty("hibernate.hbm2ddl.auto", "update");
		DatasourceConnectionProviderImpl connectionProvider = new DatasourceConnectionProviderImpl();
		connectionProvider.setDataSource(dataSource);
		connectionProvider.configure(ImmutableMap.of());
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings(cfg.getProperties()).addService(ConnectionProvider.class, connectionProvider).build();
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
