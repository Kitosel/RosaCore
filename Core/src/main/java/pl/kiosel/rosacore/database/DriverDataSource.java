package pl.kiosel.rosacore.database;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

final class DriverDataSource implements DataSource {

	private final Driver driver;
	private final String jdbcUrl;
	private final Properties properties;
	private volatile PrintWriter logWriter;
	private volatile int loginTimeout;

	DriverDataSource(Driver driver, String jdbcUrl, Properties properties) {
		this.driver = Objects.requireNonNull(driver, "driver");
		this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
		this.properties = copy(Objects.requireNonNull(properties, "properties"));
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connect(this.properties);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Properties copy = copy(this.properties);
		if (username != null) copy.setProperty("user", username);
		if (password != null) copy.setProperty("password", password);
		return connect(copy);
	}

	private Connection connect(Properties properties) throws SQLException {
		Connection connection = this.driver.connect(this.jdbcUrl, properties);
		if (connection == null) {
			throw new SQLException(this.driver.getClass().getName() + " does not accept " + this.jdbcUrl);
		}
		return connection;
	}

	@Override
	public PrintWriter getLogWriter() {
		return this.logWriter;
	}

	@Override
	public void setLogWriter(PrintWriter out) {
		this.logWriter = out;
	}

	@Override
	public void setLoginTimeout(int seconds) {
		this.loginTimeout = seconds;
	}

	@Override
	public int getLoginTimeout() {
		return this.loginTimeout;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Parent logger is not exposed by the isolated driver");
	}

	@Override
	public <T> T unwrap(Class<T> type) throws SQLException {
		if (type.isInstance(this)) return type.cast(this);
		if (type.isInstance(this.driver)) return type.cast(this.driver);
		throw new SQLException("Not a wrapper for " + type.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> type) {
		return type.isInstance(this) || type.isInstance(this.driver);
	}

	private static Properties copy(Properties source) {
		Properties copy = new Properties();
		copy.putAll(source);
		return copy;
	}
}
