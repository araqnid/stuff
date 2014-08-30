package org.araqnid.stuff;

import javax.sql.DataSource;

import org.postgresql.ds.PGPoolingDataSource;

import com.google.inject.Singleton;

@Singleton
public class PostgresqlDataSourceProviderService extends ProviderService<DataSource> {
	private PGPoolingDataSource pool;

	public PostgresqlDataSourceProviderService() {
		super(DataSource.class);
	}

	@Override
	protected void startUp() throws Exception {
		pool = new PGPoolingDataSource();
		pool.setPassword("xyzzy");
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
