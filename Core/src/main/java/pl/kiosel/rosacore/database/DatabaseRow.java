package pl.kiosel.rosacore.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class DatabaseRow {

	private final ResultSet result;

	DatabaseRow(ResultSet result) {
		this.result = Objects.requireNonNull(result, "result");
	}

	public Object get(String column) throws SQLException {
		return this.result.getObject(column);
	}

	public <T> T get(String column, Class<T> type) throws SQLException {
		Objects.requireNonNull(type, "type");
		Object value = get(column);

		if (value == null) return null;
		if (type.isInstance(value)) return type.cast(value);
		if (type == String.class) return type.cast(String.valueOf(value));

		if (value instanceof Number) {
			Number number = (Number) value;
			if (type == Integer.class) return type.cast(number.intValue());
			if (type == Long.class) return type.cast(number.longValue());
			if (type == Double.class) return type.cast(number.doubleValue());
			if (type == Float.class) return type.cast(number.floatValue());
			if (type == Short.class) return type.cast(number.shortValue());
			if (type == Byte.class) return type.cast(number.byteValue());
		}

		if (type == UUID.class) return type.cast(UUID.fromString(String.valueOf(value)));
		if (type == Instant.class) {
			if (value instanceof Timestamp) return type.cast(((Timestamp) value).toInstant());
			if (value instanceof Number) return type.cast(Instant.ofEpochMilli(((Number) value).longValue()));
		}
		throw new SQLException("Column " + column + " contains " + value.getClass().getName()
				+ ", not " + type.getName());
	}

	public String getString(String column) throws SQLException {
		return this.result.getString(column);
	}

	public int getInt(String column) throws SQLException {
		return this.result.getInt(column);
	}

	public Integer getInteger(String column) throws SQLException {
		int value = this.result.getInt(column);
		return this.result.wasNull() ? null : value;
	}

	public long getLong(String column) throws SQLException {
		return this.result.getLong(column);
	}

	public Long getLongObject(String column) throws SQLException {
		long value = this.result.getLong(column);
		return this.result.wasNull() ? null : value;
	}

	public double getDouble(String column) throws SQLException {
		return this.result.getDouble(column);
	}

	public Double getDoubleObject(String column) throws SQLException {
		double value = this.result.getDouble(column);
		return this.result.wasNull() ? null : value;
	}

	public boolean getBoolean(String column) throws SQLException {
		return this.result.getBoolean(column);
	}

	public Boolean getBooleanObject(String column) throws SQLException {
		boolean value = this.result.getBoolean(column);
		return this.result.wasNull() ? null : value;
	}

	public UUID getUuid(String column) throws SQLException {
		String value = getString(column);
		return value == null || value.isEmpty() ? null : UUID.fromString(value);
	}

	public Instant getInstant(String column) throws SQLException {
		Long value = getLongObject(column);
		return value == null ? null : Instant.ofEpochMilli(value);
	}

	public byte[] getBytes(String column) throws SQLException {
		return this.result.getBytes(column);
	}

	public boolean isNull(String column) throws SQLException {
		return get(column) == null;
	}

	public ResultSet getResultSet() {
		return this.result;
	}
}
