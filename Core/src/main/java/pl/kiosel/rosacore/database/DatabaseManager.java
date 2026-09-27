package pl.kiosel.rosacore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.library.RosaLibraryHandle;
import pl.kiosel.rosacore.library.RosaLibraryManager;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class DatabaseManager implements AutoCloseable {

	private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

	@Getter private final DatabaseSettings settings;
	@Getter private final String tablePrefix;
	private final HikariDataSource dataSource;
	private final AutoCloseable driverResource;
	private final ExecutorService executor;
	private final MigrationRunner migrationRunner;
	private final AtomicBoolean closed = new AtomicBoolean();

	private DatabaseManager(DatabaseSettings settings, String tablePrefix, Driver driver, AutoCloseable driverResource, String poolName) {
		this.settings = Objects.requireNonNull(settings, "settings");
		this.tablePrefix = MigrationRunner.validatePrefix(tablePrefix);
		this.driverResource = Objects.requireNonNull(driverResource, "driverResource");
		prepareLocalDirectory(settings);

		HikariConfig hikari = new HikariConfig();
		hikari.setPoolName(normalizePoolName(poolName));
		hikari.setDataSource(new DriverDataSource(driver, settings.createJdbcUrl(), createProperties(settings)));
		hikari.setMaximumPoolSize(settings.getPoolSize());
		hikari.setMinimumIdle(Math.min(1, settings.getPoolSize()));
		hikari.setConnectionTimeout(settings.getConnectionTimeoutMillis());
		hikari.setValidationTimeout(Math.min(5_000L, settings.getConnectionTimeoutMillis()));
		hikari.setInitializationFailTimeout(settings.getConnectionTimeoutMillis());
		hikari.setAutoCommit(true);
		this.dataSource = new HikariDataSource(hikari);
		this.executor = Executors.newFixedThreadPool(
				Math.max(1, Math.min(settings.getPoolSize(), 4)),
				new DatabaseThreadFactory(normalizePoolName(poolName))
		);
		this.migrationRunner = new MigrationRunner(this, this.tablePrefix);
	}

	public static DatabaseManager open(Plugin plugin, DatabaseSettings settings) {
		return open(plugin, settings, "");
	}

	public static DatabaseManager open(Plugin plugin, DatabaseSettings settings, String tablePrefix) {
		Objects.requireNonNull(plugin, "plugin");
		Objects.requireNonNull(settings, "settings");
		RosaLibraryHandle library = new RosaLibraryManager(plugin).load(settings.getType().getLibrary());
		try {
			Driver driver = createDriver(library, settings.getType().getDriverClassName());
			return new DatabaseManager(
					settings,
					tablePrefix,
					driver,
					library,
					plugin.getName() + '-' + settings.getType().name().toLowerCase()
			);
		} catch (RuntimeException | Error exception) {
			closeAfterFailedOpen(library, exception);
			throw exception;
		}
	}

	public Connection getConnection() throws SQLException {
		ensureOpen();
		return this.dataSource.getConnection();
	}

	public DatabaseSession session(Connection connection) {
		ensureOpen();
		return new DatabaseSession(connection, this.tablePrefix);
	}

	public String tableName(String logicalName) {
		ensureOpen();
		return this.tablePrefix + DatabaseTable.validateIdentifier(logicalName, "Table name");
	}

	public String tableName(DatabaseTable table) {
		return tableName(Objects.requireNonNull(table, "table").getName());
	}

	public void createTable(DatabaseTable table) {
		withConnection(connection -> session(connection).createTable(table));
	}

	public int executeUpdate(String sql, Object... parameters) {
		return withConnectionResult(connection -> session(connection).executeUpdate(sql, parameters));
	}

	public <T> List<T> query(String sql, DatabaseRowMapper<T> mapper, Object... parameters) {
		return withConnectionResult(connection -> session(connection).query(sql, mapper, parameters));
	}

	public void queryEach(String sql, DatabaseRowConsumer consumer, Object... parameters) {
		withConnection(connection -> session(connection).queryEach(sql, consumer, parameters));
	}

	public <T> Optional<T> queryFirst(String sql, DatabaseRowMapper<T> mapper, Object... parameters) {
		return withConnectionResult(connection -> session(connection).queryFirst(sql, mapper, parameters));
	}

	public <T> List<T> selectAll(DatabaseTable table, DatabaseRowMapper<T> mapper) {
		return withConnectionResult(connection -> session(connection).selectAll(table, mapper));
	}

	public void forEach(DatabaseTable table, DatabaseRowConsumer consumer) {
		withConnection(connection -> session(connection).forEach(table, consumer));
	}

	public <T> Optional<T> findOne(DatabaseTable table, String column, Object value,
								   DatabaseRowMapper<T> mapper) {
		return withConnectionResult(connection -> session(connection).findOne(table, column, value, mapper));
	}

	public boolean exists(DatabaseTable table, String column, Object value) {
		return withConnectionResult(connection -> session(connection).exists(table, column, value));
	}

	public long count(DatabaseTable table) {
		return withConnectionResult(connection -> session(connection).count(table));
	}

	public int insert(DatabaseTable table, DatabaseValues values) {
		return withConnectionResult(connection -> session(connection).insert(table, values));
	}

	public int update(DatabaseTable table, DatabaseValues values, String whereColumn, Object whereValue) {
		return withConnectionResult(connection ->
				session(connection).update(table, values, whereColumn, whereValue));
	}

	public int delete(DatabaseTable table, String whereColumn, Object whereValue) {
		return withConnectionResult(connection -> session(connection).delete(table, whereColumn, whereValue));
	}

	public int upsert(DatabaseTable table, DatabaseValues values) {
		return transactionResult(connection -> session(connection).upsert(table, values));
	}

	public int upsert(DatabaseTable table, DatabaseValues values, String... keyColumns) {
		String[] keys = keyColumns == null ? null : keyColumns.clone();
		return transactionResult(connection -> session(connection).upsert(table, values, keys));
	}

	public void withConnection(SqlAction action) {
		Objects.requireNonNull(action, "action");
		withConnectionResult(connection -> {
			action.execute(connection);
			return null;
		});
	}

	public <T> T withConnectionResult(SqlFunction<T> function) {
		Objects.requireNonNull(function, "function");
		ensureOpen();
		try (Connection connection = this.dataSource.getConnection()) {
			return function.execute(connection);
		} catch (Throwable throwable) {
			throw propagate("Database operation failed", throwable);
		}
	}

	public void transaction(SqlAction action) {
		Objects.requireNonNull(action, "action");
		transactionResult(connection -> {
			action.execute(connection);
			return null;
		});
	}

	public <T> T transactionResult(SqlFunction<T> function) {
		Objects.requireNonNull(function, "function");
		ensureOpen();
		try (Connection connection = this.dataSource.getConnection()) {
			boolean previousAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				T result = function.execute(connection);
				connection.commit();
				return result;
			} catch (Throwable throwable) {
				try {
					connection.rollback();
				} catch (SQLException rollbackFailure) {
					throwable.addSuppressed(rollbackFailure);
				}
				throw propagate("Database transaction failed", throwable);
			} finally {
				try {
					connection.setAutoCommit(previousAutoCommit);
				} catch (SQLException ignored) {
				}
			}
		} catch (SQLException exception) {
			throw new DatabaseException("Could not open database transaction", exception);
		}
	}

	public CompletableFuture<Void> runAsync(SqlAction action) {
		Objects.requireNonNull(action, "action");
		ensureOpen();
		return CompletableFuture.runAsync(() -> withConnection(action), this.executor);
	}

	public <T> CompletableFuture<T> supplyAsync(SqlFunction<T> function) {
		Objects.requireNonNull(function, "function");
		ensureOpen();
		return CompletableFuture.supplyAsync(() -> withConnectionResult(function), this.executor);
	}

	public synchronized MigrationResult migrate(DatabaseMigration... migrations) {
		ensureOpen();
		return this.migrationRunner.migrate(migrations);
	}

	public CompletableFuture<MigrationResult> migrateAsync(DatabaseMigration... migrations) {
		ensureOpen();
		DatabaseMigration[] copy = migrations == null ? null : migrations.clone();
		return CompletableFuture.supplyAsync(() -> migrate(copy), this.executor);
	}

	public int getSchemaVersion() {
		ensureOpen();
		return this.migrationRunner.readCurrentVersion();
	}

	public DatabaseType getType() {
		return this.settings.getType();
	}

	public boolean isClosed() {
		return this.closed.get();
	}

	@Override
	public void close() {
		if (!this.closed.compareAndSet(false, true)) return;

		this.executor.shutdown();
		try {
			if (!this.executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				this.executor.shutdownNow();
			}
		} catch (InterruptedException exception) {
			this.executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		RuntimeException failure = null;
		try {
			this.dataSource.close();
		} catch (RuntimeException exception) {
			failure = exception;
		}
		try {
			this.driverResource.close();
		} catch (Exception exception) {
			if (failure == null) failure = new DatabaseException("Could not close database driver", exception);
			else failure.addSuppressed(exception);
		}
		if (failure != null) throw failure;
	}

	private void ensureOpen() {
		if (this.closed.get()) throw new IllegalStateException("Database manager is closed");
	}

	private static Driver createDriver(RosaLibraryHandle library, String className) {
		try {
			Object instance = library.loadClass(className).getDeclaredConstructor().newInstance();
			if (!(instance instanceof Driver)) {
				throw new DatabaseException(className + " does not implement java.sql.Driver");
			}
			return (Driver) instance;
		} catch (DatabaseException exception) {
			throw exception;
		} catch (ReflectiveOperationException exception) {
			throw new DatabaseException("Could not create JDBC driver " + className, exception);
		}
	}

	private static Properties createProperties(DatabaseSettings settings) {
		Properties properties = new Properties();
		properties.setProperty("user", settings.getUsername());
		properties.setProperty("password", settings.getPassword());
		if (settings.getType().isRemote()) {
			properties.setProperty("sslMode", settings.isSsl() ? "verify-full" : "disable");
			properties.setProperty("tcpKeepAlive", "true");
			properties.setProperty("connectTimeout", Long.toString(settings.getConnectionTimeoutMillis()));
			properties.setProperty("createDatabaseIfNotExist", Boolean.toString(settings.isCreateDatabaseIfMissing()));
		}
		return properties;
	}

	private static void prepareLocalDirectory(DatabaseSettings settings) {
		if (settings.getType() != DatabaseType.H2) return;
		File parent = settings.getH2File().getAbsoluteFile().getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
			throw new DatabaseException("Could not create H2 directory " + parent);
		}
	}

	private static String normalizePoolName(String name) {
		String normalized = name == null ? "RosaCore-database" : name.replaceAll("[^A-Za-z0-9_.-]", "-");
		return normalized.isEmpty() ? "RosaCore-database" : normalized;
	}

	private static DatabaseException propagate(String message, Throwable throwable) {
		if (throwable instanceof Error) throw (Error) throwable;
		if (throwable instanceof DatabaseException) return (DatabaseException) throwable;
		return new DatabaseException(message, throwable);
	}

	private static void closeAfterFailedOpen(AutoCloseable resource, Throwable original) {
		try {
			resource.close();
		} catch (Exception closeFailure) {
			original.addSuppressed(closeFailure);
		}
	}

	private static final class DatabaseThreadFactory implements ThreadFactory {
		private final String poolName;
		private final AtomicInteger counter = new AtomicInteger();

		private DatabaseThreadFactory(String poolName) {
			this.poolName = poolName;
		}

		@Override
		public Thread newThread(Runnable task) {
			Thread thread = new Thread(task, this.poolName + "-worker-" + this.counter.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}
	}
}
