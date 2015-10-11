package org.araqnid.stuff.workqueue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.PostgresqlDataSourceProviderService;
import org.araqnid.stuff.config.ServerIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.AbstractIdleService;

public class SqlWorkQueue implements WorkQueue {
	private static final Logger LOG = LoggerFactory.getLogger(SqlWorkQueue.class);
	private static final Logger SQL_LOG = LoggerFactory.getLogger(SqlWorkQueue.class.getName() + ".SQL");

	private enum SqlCatalogue {
		CreateInsertItem, CreateInsertItemEvent, MarkInProgressUpdateItem, MarkInProgressInsertItemEvent,
		MarkProcessedUpdateItem, MarkProcessedInsertItemEvent, MarkFailedUpdateItem, MarkFailedInsertItemEvent,

		SetupCreateSchema, SetupCreateTableItemStatus, SetupCreateTableItemEventType, SetupCreateTableItem,
		SetupCreateTableItemEvent, SetupPopulateItemStatus, SetupPopulateItemEventType
	};

	private static final Map<SqlCatalogue, CompiledSql> sqlCatalogue;
	static {
		try {
			URL resource = Resources.getResource(SqlWorkQueue.class, "SqlWorkQueue.properties");
			ByteSource byteSource = Resources.asByteSource(resource);
			final Properties properties = new Properties();
			try (InputStream instream = byteSource.openStream()) {
				properties.load(instream);
			}
			sqlCatalogue = Maps.toMap(Arrays.asList(SqlCatalogue.values()), new Function<SqlCatalogue, CompiledSql>() {
				@Override
				public CompiledSql apply(SqlCatalogue input) {
					String value = properties.getProperty(input.toString());
					if (value == null) throw new RuntimeException("No value for " + input);
					return new CompiledSql(value);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Unable to load SqlWorkQueue.properties", e);
		}
	}

	private final String queueCode;
	private final Accessor accessor;
	private final ObjectMapper objectMapper;
	private final AppVersion appVersion;
	private final UUID instanceId;
	private final String hostname;

	public SqlWorkQueue(String queueCode,
			Accessor accessor,
			ObjectMapper objectMapper,
			AppVersion appVersion,
			@ServerIdentity UUID instanceId,
			@ServerIdentity String hostname) {
		this.queueCode = queueCode;
		this.accessor = accessor;
		this.objectMapper = objectMapper;
		this.appVersion = appVersion;
		this.instanceId = instanceId;
		this.hostname = hostname;
	}

	public String context() {
		try {
			return objectMapper.writer().writeValueAsString(
					ImmutableMap.<String, Object> of("app_version", Optional.fromNullable(appVersion.version),
							"instance_id", instanceId, "host", hostname, "thread", Thread.currentThread().getName()));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void create(String entryId, byte[] payload) {
		LOG.debug("{} create {} {} bytes", queueCode, entryId, payload.length);
		accessor.doSql("SqlWorkQueue#create", Sql.exec(SqlCatalogue.CreateInsertItem).with("queue_code", queueCode)
				.with("reference", entryId).with("payload", Optional.of(payload)),
				Sql.exec(SqlCatalogue.CreateInsertItemEvent).with("queue_code", queueCode).with("reference", entryId)
						.with("context", context()));
	}

	@Override
	public void markInProgress(String entryId) {
		LOG.debug("{} in-progress {}", queueCode, entryId);
		accessor.doSql(
				"SqlWorkQueue#markInProgress",
				Sql.exec(SqlCatalogue.MarkInProgressUpdateItem).with("queue_code", queueCode)
						.with("reference", entryId),
				Sql.exec(SqlCatalogue.MarkInProgressInsertItemEvent).with("queue_code", queueCode)
						.with("reference", entryId).with("context", context()));
	}

	@Override
	public void markProcessed(String entryId) {
		LOG.debug("{} processed {}", queueCode, entryId);
		accessor.doSql(
				"SqlWorkQueue#markProcessed",
				Sql.exec(SqlCatalogue.MarkProcessedUpdateItem).with("queue_code", queueCode).with("reference", entryId),
				Sql.exec(SqlCatalogue.MarkProcessedInsertItemEvent).with("queue_code", queueCode)
						.with("reference", entryId).with("context", context()));
	}

	@Override
	public void markFailed(String entryId, boolean permanent, String message, Throwable t) {
		LOG.debug("{} failed {} {} {}", queueCode, entryId, permanent ? "permanent" : "temporary",
				message != null ? message : t != null ? t.toString() : "<no message>");
		accessor.doSql(
				"SqlWorkQueue#markFailed",
				Sql.exec(SqlCatalogue.MarkFailedUpdateItem).with("queue_code", queueCode).with("reference", entryId),
				Sql.exec(SqlCatalogue.MarkFailedInsertItemEvent).with("queue_code", queueCode)
						.with("reference", entryId).with("context", context()));
	}

	@Override
	public String toString() {
		return queueCode;
	}

	public static class Accessor {
		private final DataSource dataSource;

		@Inject
		public Accessor(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		public void doSql(String caller, Sql... commands) {
			try (Connection conn = dataSource.getConnection()) {
				doSqlTransaction(caller, conn, commands);
			} catch (SQLException e) {
				for (SQLException se = e; se != null; se = se.getNextException()) {
					LOG.error(
							"{}",
							Joiner.on(": ").join(
									Iterables.filter(Arrays.asList(se.getSQLState(),
											se.getErrorCode() > 0 ? Integer.toString(se.getErrorCode()) : null, se
													.getMessage().replace("\n", " // ")), Predicates.notNull())));
				}
				throw new RuntimeException("Unhandled SQL exception updating work queue: " + caller, e);
			}
		}

		private void doSqlTransaction(String caller, Connection conn, Sql... commands) throws SQLException {
			boolean completed = false;
			try {
				SQL_LOG.debug("BEGIN");
				conn.setAutoCommit(false);
				doSqlInTransaction(caller, conn, commands);
				completed = true;
			} finally {
				if (completed) {
					SQL_LOG.debug("COMMIT");
					conn.commit();
				}
				else {
					SQL_LOG.debug("ROLLBACK");
					conn.rollback();
				}
			}
		}

		private void doSqlInTransaction(String caller, Connection conn, Sql... commands) throws SQLException {
			for (Sql command : commands) {
				doSqlCommand(conn, command);
			}
		}

		private void doSqlCommand(Connection conn, Sql command) throws SQLException {
			final CompiledSql sql = command.sql();
			try (PreparedStatement stmt = conn.prepareStatement(sql.edited)) {
				SQL_LOG.debug("{}", sql.edited);
				int index = 1;
				for (String placeholder : sql.placeholders) {
					Sql.Binder binder = command.values.get(placeholder);
					if (binder == null)
						throw new RuntimeException("No value set for placeholder \"" + placeholder + "\"");
					binder.bind(stmt, index++);
				}
				int rpc = stmt.executeUpdate();
				LOG.debug("{} rows processed", rpc);
			}
		}
	}

	public static class Setup extends AbstractIdleService {
		private final Provider<Accessor> accessorProvider;
		private final PostgresqlDataSourceProviderService dataSourceService;

		@Inject
		public Setup(Provider<Accessor> accessorProvider, PostgresqlDataSourceProviderService dataSourceService) {
			this.accessorProvider = accessorProvider;
			this.dataSourceService = dataSourceService;
		}

		@Override
		protected void startUp() throws Exception {
			dataSourceService.awaitRunning();
			Accessor accessor = accessorProvider.get();
			ImmutableList<SqlCatalogue> commands = ImmutableList.of(SqlCatalogue.SetupCreateSchema,
					SqlCatalogue.SetupCreateTableItemStatus, SqlCatalogue.SetupCreateTableItemEventType,
					SqlCatalogue.SetupCreateTableItem, SqlCatalogue.SetupCreateTableItemEvent,
					SqlCatalogue.SetupPopulateItemStatus, SqlCatalogue.SetupPopulateItemEventType);
			accessor.doSql("Setup", Iterables.toArray(Iterables.transform(commands, new Function<SqlCatalogue, Sql>() {
				@Override
				public Sql apply(SqlCatalogue input) {
					return Sql.exec(input);
				}
			}), Sql.class));
		}

		@Override
		protected void shutDown() throws Exception {
		}
	}

	private static class Sql {
		private final SqlCatalogue sql;
		private final Map<String, Binder> values = new HashMap<>();

		private Sql(SqlCatalogue sql) {
			this.sql = sql;
		}

		public static Sql exec(SqlCatalogue sql) {
			return new Sql(sql);
		}

		public Sql with(String name, final String value) {
			Preconditions.checkNotNull(value);
			values.put(name, new Binder() {
				@Override
				public void bind(PreparedStatement stmt, int index) throws SQLException {
					stmt.setString(index, value);
				}
			});
			return this;
		}

		public Sql with(String name, final Optional<byte[]> value) {
			if (!value.isPresent()) return withNull(name, Types.VARBINARY);
			values.put(name, new Binder() {
				@Override
				public void bind(PreparedStatement stmt, int index) throws SQLException {
					stmt.setBytes(index, value.get());
				}
			});
			return this;
		}

		public Sql withNull(String name, final int sqlType) {
			values.put(name, new Binder() {
				@Override
				public void bind(PreparedStatement stmt, int index) throws SQLException {
					stmt.setNull(index, sqlType);
				}
			});
			return this;
		}

		public CompiledSql sql() {
			return sqlCatalogue.get(sql);
		}

		@Override
		public String toString() {
			return sql.toString();
		}

		public interface Binder {
			void bind(PreparedStatement stmt, int index) throws SQLException;
		}
	}

	private static class CompiledSql {
		private final String original;
		private final String edited;
		private final List<String> placeholders;

		public CompiledSql(String original) {
			this.original = original;
			Pattern pattern = Pattern.compile(":([A-Za-z_][A-Za-z_0-9]*)");
			Matcher matcher = pattern.matcher(original);
			List<String> found = new ArrayList<String>();
			int pos = 0;
			StringBuilder builder = new StringBuilder(original.length());
			while (matcher.find()) {
				builder.append(original.substring(pos, matcher.start()));
				found.add(matcher.group(1));
				builder.append("/* ").append(matcher.group(0)).append(" */ ?");
				pos = matcher.end();
			}
			builder.append(original.substring(pos, original.length()));
			edited = builder.toString();
			placeholders = ImmutableList.copyOf(found);
		}

		@Override
		public String toString() {
			return original;
		}
	}
}
