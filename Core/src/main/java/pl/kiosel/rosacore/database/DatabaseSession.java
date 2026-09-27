package pl.kiosel.rosacore.database;

import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public final class DatabaseSession {

	@Getter private final Connection connection;
	private final String tablePrefix;

	DatabaseSession(Connection connection, String tablePrefix) {
		this.connection = Objects.requireNonNull(connection, "connection");
		this.tablePrefix = MigrationRunner.validatePrefix(tablePrefix);
	}

	public String tableName(DatabaseTable table) {
		return tableName(Objects.requireNonNull(table, "table").getName());
	}

	public String tableName(String logicalName) {
		return this.tablePrefix + DatabaseTable.validateIdentifier(logicalName, "Table name");
	}

	public void createTable(DatabaseTable table) {
		executeUpdate(Objects.requireNonNull(table, "table").createSql(tableName(table)));
	}

	public int executeUpdate(String sql, Object... parameters) {
		try (PreparedStatement statement = prepare(sql, parameters)) {
			return statement.executeUpdate();
		} catch (SQLException exception) {
			throw new DatabaseException("Database update failed: " + summarize(sql), exception);
		}
	}

	public <T> List<T> query(String sql, DatabaseRowMapper<T> mapper, Object... parameters) {
		Objects.requireNonNull(mapper, "mapper");
		List<T> values = new ArrayList<>();
		queryEach(sql, row -> values.add(mapper.map(row)), parameters);
		return values;
	}

	public void queryEach(String sql, DatabaseRowConsumer consumer, Object... parameters) {
		Objects.requireNonNull(consumer, "consumer");
		try (PreparedStatement statement = prepare(sql, parameters);
			 ResultSet result = statement.executeQuery()) {
			DatabaseRow row = new DatabaseRow(result);
			while (result.next()) consumer.accept(row);
		} catch (DatabaseException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new DatabaseException("Database query failed: " + summarize(sql), exception);
		}
	}

	public <T> Optional<T> queryFirst(String sql, DatabaseRowMapper<T> mapper, Object... parameters) {
		Objects.requireNonNull(mapper, "mapper");
		try (PreparedStatement statement = prepare(sql, parameters);
			 ResultSet result = statement.executeQuery()) {
			return result.next() ? Optional.ofNullable(mapper.map(new DatabaseRow(result))) : Optional.empty();
		} catch (DatabaseException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new DatabaseException("Database query failed: " + summarize(sql), exception);
		}
	}

	public <T> List<T> selectAll(DatabaseTable table, DatabaseRowMapper<T> mapper) {
		return query("SELECT * FROM " + tableName(table), mapper);
	}

	public void forEach(DatabaseTable table, DatabaseRowConsumer consumer) {
		queryEach("SELECT * FROM " + tableName(table), consumer);
	}

	public <T> Optional<T> findOne(DatabaseTable table, String column, Object value,
								   DatabaseRowMapper<T> mapper) {
		String identifier = DatabaseTable.validateIdentifier(column, "Column name");
		String sql = "SELECT * FROM " + tableName(table) + " WHERE " + identifier;
		if (value == null) return queryFirst(sql + " IS NULL", mapper);
		return queryFirst(sql + " = ?", mapper, value);
	}

	public boolean exists(DatabaseTable table, String column, Object value) {
		String identifier = DatabaseTable.validateIdentifier(column, "Column name");
		String sql = "SELECT 1 FROM " + tableName(table) + " WHERE " + identifier;
		if (value == null) return queryFirst(sql + " IS NULL", row -> Boolean.TRUE).isPresent();
		return queryFirst(sql + " = ?", row -> Boolean.TRUE, value).isPresent();
	}

	public long count(DatabaseTable table) {
		return queryFirst("SELECT COUNT(*) AS rosa_count FROM " + tableName(table),
				row -> row.getLong("rosa_count")).orElse(0L);
	}

	public int insert(DatabaseTable table, DatabaseValues values) {
		Map<String, Object> entries = requireValues(values);
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName(table)).append(" (");
		StringBuilder placeholders = new StringBuilder();
		List<Object> parameters = new ArrayList<>();
		for (Map.Entry<String, Object> entry : entries.entrySet()) {
			if (parameters.size() > 0) {
				sql.append(", ");
				placeholders.append(", ");
			}
			sql.append(entry.getKey());
			placeholders.append('?');
			parameters.add(entry.getValue());
		}
		sql.append(") VALUES (").append(placeholders).append(')');
		return executeUpdate(sql.toString(), parameters.toArray());
	}

	public int update(DatabaseTable table, DatabaseValues values, String whereColumn, Object whereValue) {
		String key = DatabaseTable.validateIdentifier(whereColumn, "Where column");
		return updateWhere(table, requireValues(values), Collections.singletonMap(key, whereValue));
	}

	public int delete(DatabaseTable table, String whereColumn, Object whereValue) {
		String key = DatabaseTable.validateIdentifier(whereColumn, "Where column");
		String sql = "DELETE FROM " + tableName(table) + " WHERE " + key;
		if (whereValue == null) return executeUpdate(sql + " IS NULL");
		return executeUpdate(sql + " = ?", whereValue);
	}

	public int upsert(DatabaseTable table, DatabaseValues values) {
		Objects.requireNonNull(table, "table");
		if (table.getPrimaryKey().isEmpty()) {
			throw new IllegalArgumentException("Table " + table.getName() + " has no primary key");
		}
		return upsert(table, values, table.getPrimaryKey().toArray(new String[0]));
	}

	public int upsert(DatabaseTable table, DatabaseValues values, String... keyColumns) {
		Map<String, Object> allValues = requireValues(values);
		List<String> keys = validateKeys(keyColumns);
		LinkedHashMap<String, Object> conditions = new LinkedHashMap<>();
		LinkedHashMap<String, Object> updates = new LinkedHashMap<>(allValues);
		for (String key : keys) {
			if (!allValues.containsKey(key)) {
				throw new IllegalArgumentException("Missing upsert key value " + key);
			}
			conditions.put(key, allValues.get(key));
			updates.remove(key);
		}

		int updated = updates.isEmpty() ? 0 : updateWhere(table, updates, conditions);
		if (updated > 0 || existsWhere(table, conditions)) return updated;
		return insert(table, values);
	}

	private int updateWhere(DatabaseTable table, Map<String, Object> values,
							Map<String, Object> conditions) {
		if (values.isEmpty()) throw new IllegalArgumentException("Update values cannot be empty");
		if (conditions.isEmpty()) throw new IllegalArgumentException("Update conditions cannot be empty");
		StringBuilder sql = new StringBuilder("UPDATE ").append(tableName(table)).append(" SET ");
		List<Object> parameters = new ArrayList<>();
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			if (!parameters.isEmpty()) sql.append(", ");
			sql.append(entry.getKey()).append(" = ?");
			parameters.add(entry.getValue());
		}
		appendConditions(sql, parameters, conditions);
		return executeUpdate(sql.toString(), parameters.toArray());
	}

	private boolean existsWhere(DatabaseTable table, Map<String, Object> conditions) {
		StringBuilder sql = new StringBuilder("SELECT 1 FROM ").append(tableName(table));
		List<Object> parameters = new ArrayList<>();
		appendConditions(sql, parameters, conditions);
		return queryFirst(sql.toString(), row -> Boolean.TRUE, parameters.toArray()).isPresent();
	}

	private static void appendConditions(StringBuilder sql, List<Object> parameters,
										 Map<String, Object> conditions) {
		sql.append(" WHERE ");
		int index = 0;
		for (Map.Entry<String, Object> condition : conditions.entrySet()) {
			if (index++ > 0) sql.append(" AND ");
			sql.append(condition.getKey());
			if (condition.getValue() == null) {
				sql.append(" IS NULL");
			} else {
				sql.append(" = ?");
				parameters.add(condition.getValue());
			}
		}
	}

	private PreparedStatement prepare(String sql, Object... parameters) throws SQLException {
		String statementSql = requireSql(sql);
		PreparedStatement statement = this.connection.prepareStatement(statementSql);
		try {
			Object[] values = parameters == null ? new Object[0] : parameters;
			for (int index = 0; index < values.length; index++) {
				statement.setObject(index + 1, normalize(values[index]));
			}
			return statement;
		} catch (SQLException | RuntimeException exception) {
			try {
				statement.close();
			} catch (SQLException closeFailure) {
				exception.addSuppressed(closeFailure);
			}
			throw exception;
		}
	}

	private static Object normalize(Object value) {
		if (value instanceof UUID) return value.toString();
		if (value instanceof Instant) return ((Instant) value).toEpochMilli();
		if (value instanceof Enum<?>) return ((Enum<?>) value).name();
		if (value instanceof Character) return value.toString();
		return value;
	}

	private static Map<String, Object> requireValues(DatabaseValues values) {
		Objects.requireNonNull(values, "values");
		if (values.isEmpty()) throw new IllegalArgumentException("Database values cannot be empty");
		return values.asMap();
	}

	private static List<String> validateKeys(String[] keyColumns) {
		Objects.requireNonNull(keyColumns, "keyColumns");
		if (keyColumns.length == 0) throw new IllegalArgumentException("Upsert key cannot be empty");
		List<String> keys = new ArrayList<>();
		for (String key : Arrays.asList(keyColumns)) {
			String identifier = DatabaseTable.validateIdentifier(key, "Upsert key column");
			if (!keys.contains(identifier)) keys.add(identifier);
		}
		return keys;
	}

	private static String requireSql(String sql) {
		String value = sql == null ? "" : sql.trim();
		if (value.isEmpty()) throw new IllegalArgumentException("SQL cannot be empty");
		return value;

	}

	private static String summarize(String sql) {
		String value = sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
		return value.length() <= 160 ? value : value.substring(0, 157) + "...";
	}
}
