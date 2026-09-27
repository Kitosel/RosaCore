package pl.kiosel.rosacore.database;

import lombok.Getter;
import pl.kiosel.rosacore.config.ConfigView;

import java.io.File;
import java.util.Objects;

@Getter
public final class DatabaseSettings {

	public static final int DEFAULT_POOL_SIZE = 5;
	public static final long DEFAULT_CONNECTION_TIMEOUT_MILLIS = 10_000L;

	private final DatabaseType type;
	private final File h2File;
	private final String host;
	private final int port;
	private final String database;
	private final String username;
	private final String password;
	private final int poolSize;
	private final boolean ssl;
	private final long connectionTimeoutMillis;
	private final boolean createDatabaseIfMissing;

	public DatabaseSettings(DatabaseType type, File h2File,
							String host, int port,
							String database, String username, String password,
							int poolSize, boolean ssl, long connectionTimeoutMillis) {
		this(type, h2File, host, port, database, username, password, poolSize, ssl,
				connectionTimeoutMillis, false);
	}

	private DatabaseSettings(DatabaseType type, File h2File, String host, int port,
							 String database, String username, String password, int poolSize,
							 boolean ssl, long connectionTimeoutMillis, boolean createDatabaseIfMissing) {
		this.type = Objects.requireNonNull(type, "type");
		this.h2File = h2File == null ? null : h2File.getAbsoluteFile();
		this.host = normalize(host);
		this.port = port;
		this.database = normalize(database);
		this.username = username == null ? "" : username;
		this.password = password == null ? "" : password;
		this.poolSize = poolSize;
		this.ssl = ssl;
		this.connectionTimeoutMillis = connectionTimeoutMillis;
		this.createDatabaseIfMissing = createDatabaseIfMissing;
		validate();
	}

	public static DatabaseSettings h2(File file) {
		return h2(file, DEFAULT_POOL_SIZE);
	}

	public static DatabaseSettings h2(File file, int poolSize) {
		return new DatabaseSettings(
				DatabaseType.H2,
				Objects.requireNonNull(file, "file"),
				null,
				0,
				null,
				"sa",
				"",
				poolSize,
				false,
				DEFAULT_CONNECTION_TIMEOUT_MILLIS
		);
	}

	public static DatabaseSettings mysql(String host, int port,
										 String database, String username, String password) {
		return remote(DatabaseType.MYSQL, host, port, database, username, password, false, DEFAULT_POOL_SIZE);
	}

	public static DatabaseSettings mariadb(String host, int port,
										   String database, String username, String password) {
		return remote(DatabaseType.MARIADB, host, port, database, username, password, false, DEFAULT_POOL_SIZE);
	}

	public static DatabaseSettings remote(DatabaseType type, String host, int port,
										  String database, String username, String password,
										  boolean ssl, int poolSize) {
		if (type == null || !type.isRemote()) {
			throw new IllegalArgumentException("Remote settings require MYSQL or MARIADB");
		}
		return new DatabaseSettings(
				type,
				null,
				host,
				port,
				database,
				username,
				password,
				poolSize,
				ssl,
				DEFAULT_CONNECTION_TIMEOUT_MILLIS
		);
	}

	public static DatabaseSettings fromConfig(ConfigView config, String path, File pluginDataFolder) {
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(pluginDataFolder, "pluginDataFolder");
		String root = normalizePath(path);
		DatabaseType type = DatabaseType.match(config.getString(key(root, "type"), "H2"));
		if (type == null) {
			throw new IllegalArgumentException("Unsupported database type at " + key(root, "type"));
		}

		int poolSize = config.getInt(key(root, "pool-size"), DEFAULT_POOL_SIZE);
		long timeout = config.getLong(
				key(root, "connection-timeout-ms"),
				DEFAULT_CONNECTION_TIMEOUT_MILLIS
		);
		if (type == DatabaseType.H2) {
			String fileName = config.getString(key(root, "file"), "database/database");
			return new DatabaseSettings(
					type,
					new File(pluginDataFolder, fileName),
					null,
					0,
					null,
					"sa",
					config.getString(key(root, "password"), ""),
					poolSize,
					false,
					timeout
			);
		}

		return new DatabaseSettings(
				type,
				null,
				config.getString(key(root, "host"), "localhost"),
				config.getInt(key(root, "port"), type.getDefaultPort()),
				config.getString(key(root, "database"), "database"),
				config.getString(key(root, "username"), "root"),
				config.getString(key(root, "password"), ""),
				poolSize,
				config.getBoolean(key(root, "ssl"), false),
				timeout
		).withCreateDatabaseIfMissing(config.getBoolean(key(root, "create-database"), false));
	}

	public DatabaseSettings withCreateDatabaseIfMissing(boolean enabled) {
		return new DatabaseSettings(type, h2File, host, port, database, username, password,
				poolSize, ssl, connectionTimeoutMillis, enabled);
	}

	String createJdbcUrl() {
		if (this.type == DatabaseType.H2) {
			String normalizedPath = this.h2File.toPath().toAbsolutePath().normalize().toString().replace('\\', '/');
			return "jdbc:h2:file:" + normalizedPath
					+ ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE";
		}
		return "jdbc:mariadb://" + this.host + ':' + this.port + '/' + this.database;
	}

	private void validate() {
		if (this.poolSize < 1) {
			throw new IllegalArgumentException("Pool size must be at least 1");
		}
		if (this.connectionTimeoutMillis < 250L) {
			throw new IllegalArgumentException("Connection timeout must be at least 250 ms");
		}
		if (this.type == DatabaseType.H2) {
			if (this.h2File == null) throw new IllegalArgumentException("H2 file is required");
			return;
		}
		if (this.host == null || this.host.isEmpty()) throw new IllegalArgumentException("Database host is required");
		if (this.port < 1 || this.port > 65535) throw new IllegalArgumentException("Invalid database port");
		if (this.database == null || this.database.isEmpty()) {
			throw new IllegalArgumentException("Database name is required");
		}
	}

	private static String normalize(String value) {
		return value == null ? null : value.trim();
	}

	private static String normalizePath(String path) {
		if (path == null) return "";
		String result = path.trim();
		while (result.endsWith(".")) result = result.substring(0, result.length() - 1);
		return result;
	}

	private static String key(String root, String key) {
		return root.isEmpty() ? key : root + '.' + key;
	}
}
