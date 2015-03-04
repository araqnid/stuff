package org.araqnid.stuff;

import javax.sql.DataSource;

import org.araqnid.stuff.services.ProviderService;
import org.postgresql.ds.PGPoolingDataSource;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class PostgresqlDataSourceProviderService extends ProviderService<DataSource> {
	private PGPoolingDataSource pool;
	private final Optional<String> username;
	private final Optional<String> password;
	private final Optional<String> databaseName;

	@Inject
	public PostgresqlDataSourceProviderService(@Named("pgUser") Optional<String> username,
			@Named("pgPassword") Optional<String> password,
			@Named("pgDatabase") Optional<String> databaseName) {
		super(DataSource.class);
		this.username = username;
		this.password = password;
		this.databaseName = databaseName;
	}

	@Override
	protected void startUp() throws Exception {
		pool = new PGPoolingDataSource();
		if (username.isPresent()) pool.setUser(username.get());
		if (password.isPresent()) pool.setPassword(password.get());
		if (databaseName.isPresent()) pool.setDatabaseName(databaseName.get());
	}

	@Override
	protected DataSource getValue() {
		return pool;
	}

	@Override
	protected void shutDown() throws Exception {
		pool.close();
	}
}
