package pl.kiosel.rosacore.database;

import lombok.Getter;
import pl.kiosel.rosacore.library.RosaLibrary;

import java.util.Locale;

@Getter
public enum DatabaseType {

	H2(RosaLibrary.H2, "org.h2.Driver", 0),
	MYSQL(RosaLibrary.MARIADB, "org.mariadb.jdbc.Driver", 3306),
	MARIADB(RosaLibrary.MARIADB, "org.mariadb.jdbc.Driver", 3306);

	private final RosaLibrary library;
	private final String driverClassName;
	private final int defaultPort;

	DatabaseType(RosaLibrary library, String driverClassName, int defaultPort) {
		this.library = library;
		this.driverClassName = driverClassName;
		this.defaultPort = defaultPort;
	}

	public boolean isRemote() {
		return this != H2;
	}

	public static DatabaseType match(String value) {
		if (value == null) return null;
		String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
		if (normalized.equals("MARIA_DB")) normalized = "MARIADB";
		try {
			return valueOf(normalized);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
