package pl.kiosel.rosacore.database;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class DatabaseColumn {

	private final Type type;
	private final int length;
	private final boolean nullable;

	private DatabaseColumn(Type type, int length, boolean nullable) {
		this.type = Objects.requireNonNull(type, "type");
		this.length = length;
		this.nullable = nullable;
	}

	public static DatabaseColumn varchar(int length) {
		if (length < 1) throw new IllegalArgumentException("VARCHAR length must be positive");
		return new DatabaseColumn(Type.VARCHAR, length, true);
	}

	public static DatabaseColumn text() {
		return new DatabaseColumn(Type.TEXT, -1, true);
	}

	public static DatabaseColumn integer() {
		return new DatabaseColumn(Type.INTEGER, -1, true);
	}

	public static DatabaseColumn bigint() {
		return new DatabaseColumn(Type.BIGINT, -1, true);
	}

	public static DatabaseColumn bool() {
		return new DatabaseColumn(Type.BOOLEAN, -1, true);
	}

	public static DatabaseColumn decimal() {
		return new DatabaseColumn(Type.DOUBLE, -1, true);
	}

	public static DatabaseColumn binary() {
		return new DatabaseColumn(Type.BLOB, -1, true);
	}

	public DatabaseColumn notNull() {
		return this.nullable ? new DatabaseColumn(this.type, this.length, false) : this;
	}

	public DatabaseColumn nullable() {
		return this.nullable ? this : new DatabaseColumn(this.type, this.length, true);
	}

	String toSql() {
		String sql = this.type.sql;
		if (this.type == Type.VARCHAR) sql += '(' + Integer.toString(this.length) + ')';
		return this.nullable ? sql : sql + " NOT NULL";
	}

	public enum Type {
		VARCHAR("VARCHAR"),
		TEXT("TEXT"),
		INTEGER("INT"),
		BIGINT("BIGINT"),
		BOOLEAN("BOOLEAN"),
		DOUBLE("DOUBLE"),
		BLOB("BLOB");

		private final String sql;

		Type(String sql) {
			this.sql = sql;
		}
	}
}
