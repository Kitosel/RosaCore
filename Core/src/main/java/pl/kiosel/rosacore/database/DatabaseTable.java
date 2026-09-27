package pl.kiosel.rosacore.database;

import lombok.Getter;

import java.util.*;

@Getter
public final class DatabaseTable {

	private final String name;
	private final Map<String, DatabaseColumn> columns;
	private final List<String> primaryKey;

	private DatabaseTable(String name, Map<String, DatabaseColumn> columns, List<String> primaryKey) {
		this.name = validateIdentifier(name, "Table name");
		this.columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
		this.primaryKey = Collections.unmodifiableList(new ArrayList<>(primaryKey));
	}

	public static DatabaseTable named(String name) {
		return new DatabaseTable(name, Collections.emptyMap(), Collections.emptyList());
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	public boolean hasSchema() {
		return !this.columns.isEmpty();
	}

	String createSql(String resolvedName) {
		if (this.columns.isEmpty()) {
			throw new IllegalStateException("Table " + this.name + " has no column definitions");
		}
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
				.append(resolvedName).append(" (");
		boolean first = true;
		for (Map.Entry<String, DatabaseColumn> entry : this.columns.entrySet()) {
			if (!first) sql.append(", ");
			first = false;
			sql.append(entry.getKey()).append(' ').append(entry.getValue().toSql());
		}
		if (!this.primaryKey.isEmpty()) {
			sql.append(", PRIMARY KEY (");
			for (int index = 0; index < this.primaryKey.size(); index++) {
				if (index > 0) sql.append(", ");
				sql.append(this.primaryKey.get(index));
			}
			sql.append(')');
		}
		return sql.append(')').toString();
	}

	static String validateIdentifier(String value, String description) {
		String identifier = value == null ? "" : value.trim();
		if (!identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			throw new IllegalArgumentException(description
					+ " may only contain letters, digits and underscores and cannot start with a digit: " + value);
		}
		return identifier;
	}

	public static final class Builder {

		private final String name;
		private final LinkedHashMap<String, DatabaseColumn> columns = new LinkedHashMap<>();
		private final List<String> primaryKey = new ArrayList<>();

		private Builder(String name) {
			this.name = validateIdentifier(name, "Table name");
		}

		public Builder column(String name, DatabaseColumn column) {
			String identifier = validateIdentifier(name, "Column name");
			DatabaseColumn previous = this.columns.put(identifier, Objects.requireNonNull(column, "column"));
			if (previous != null) {
				this.columns.put(identifier, previous);
				throw new IllegalArgumentException("Duplicate column " + identifier + " in table " + this.name);
			}
			return this;
		}

		public Builder primaryKey(String... columns) {
			Objects.requireNonNull(columns, "columns");
			if (columns.length == 0) throw new IllegalArgumentException("Primary key cannot be empty");
			this.primaryKey.clear();
			for (String column : Arrays.asList(columns)) {
				String identifier = validateIdentifier(column, "Primary key column");
				if (!this.primaryKey.contains(identifier)) this.primaryKey.add(identifier);
			}
			return this;
		}

		public DatabaseTable build() {
			if (this.columns.isEmpty()) {
				throw new IllegalStateException("Table " + this.name + " must contain at least one column");
			}
			for (String key : this.primaryKey) {
				if (!this.columns.containsKey(key)) {
					throw new IllegalStateException("Primary key column " + key
							+ " is not defined in table " + this.name);
				}
			}
			return new DatabaseTable(this.name, this.columns, this.primaryKey);
		}
	}
}
