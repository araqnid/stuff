package org.araqnid.stuff;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.araqnid.stuff.services.ProviderService;
import org.postgresql.ds.PGPoolingDataSource;

@Singleton
public class PostgresqlDataSourceProviderService extends ProviderService<DataSource> {
	private PGPoolingDataSource pool;
	@Nullable private final String username;
	@Nullable private final String password;
	@Nullable private final String databaseName;

	@Inject
	public PostgresqlDataSourceProviderService(@Named("pgUser") Optional<String> username,
			@Named("pgPassword") Optional<String> password,
			@Named("pgDatabase") Optional<String> databaseName) {
		super(DataSource.class);
		this.username = username.orElse(null);
		this.password = password.orElse(null);
		this.databaseName = databaseName.orElse(null);
	}

	@Override
	protected void startUp() throws Exception {
		pool = new PGPoolingDataSource();
		if (username != null) pool.setUser(username);
		if (password != null) pool.setPassword(password);
		if (databaseName != null) pool.setDatabaseName(databaseName);
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
