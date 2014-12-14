package org.araqnid.stuff;

import javax.sql.DataSource;

import org.postgresql.ds.PGPoolingDataSource;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class PostgresqlDataSourceProviderService extends AbstractIdleService implements Provider<DataSource> {
	private PGPoolingDataSource pool;

	@Inject
	public PostgresqlDataSourceProviderService(@Named("pgUser") Optional<String> username,
			@Named("pgPassword") Optional<String> password,
			@Named("pgDatabase") Optional<String> databaseName) {
		pool = new PGPoolingDataSource();
		if (username.isPresent()) pool.setUser(username.get());
		if (password.isPresent()) pool.setPassword(password.get());
		if (databaseName.isPresent()) pool.setDatabaseName(databaseName.get());
	}

	@Override
	protected void startUp() throws Exception {
	}

	@Override
	public DataSource get() {
		return pool;
	}

	@Override
	protected void shutDown() throws Exception {
		pool.close();
	}
}
