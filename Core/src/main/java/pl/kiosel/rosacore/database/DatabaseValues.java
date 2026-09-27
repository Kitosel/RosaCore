package pl.kiosel.rosacore.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DatabaseValues {

	private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

	private DatabaseValues() {
	}

	public static DatabaseValues create() {
		return new DatabaseValues();
	}

	public static DatabaseValues of(String column, Object value) {
		return create().set(column, value);
	}

	public DatabaseValues set(String column, Object value) {
		this.values.put(DatabaseTable.validateIdentifier(column, "Column name"), value);
		return this;
	}

	public DatabaseValues setIfNotNull(String column, Object value) {
		if (value != null) set(column, value);
		return this;
	}

	public DatabaseValues remove(String column) {
		this.values.remove(DatabaseTable.validateIdentifier(column, "Column name"));
		return this;
	}

	public Object get(String column) {
		return this.values.get(DatabaseTable.validateIdentifier(column, "Column name"));
	}

	public boolean contains(String column) {
		return this.values.containsKey(DatabaseTable.validateIdentifier(column, "Column name"));
	}

	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	public int size() {
		return this.values.size();
	}

	public Map<String, Object> asMap() {
		return Collections.unmodifiableMap(this.values);
	}
}
