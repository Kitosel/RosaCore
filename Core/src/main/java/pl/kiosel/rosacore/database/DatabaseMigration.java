package pl.kiosel.rosacore.database;

import java.sql.Connection;

public abstract class DatabaseMigration {

	private final int version;

	protected DatabaseMigration(int version) {
		if (version < 1) throw new IllegalArgumentException("Migration version must be positive");
		this.version = version;
	}

	public final int getVersion() {
		return this.version;
	}

	public abstract void migrate(Connection connection, String tablePrefix) throws Exception;
}
