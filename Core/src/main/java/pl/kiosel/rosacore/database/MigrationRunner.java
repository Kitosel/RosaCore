package pl.kiosel.rosacore.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MigrationRunner {

    private static final String HISTORY_SUFFIX = "rosa_schema_history";

    private final DatabaseManager database;
    private final String tablePrefix;
    private final String historyTable;

    MigrationRunner(DatabaseManager database, String tablePrefix) {
        this.database = database;
        this.tablePrefix = validatePrefix(tablePrefix);
        this.historyTable = this.tablePrefix + HISTORY_SUFFIX;
    }

    MigrationResult migrate(DatabaseMigration... migrations) {
        List<DatabaseMigration> ordered = validateAndOrder(migrations);
        int previousVersion = readCurrentVersion();
        int currentVersion = previousVersion;
        List<Integer> applied = new ArrayList<>();

        for (DatabaseMigration migration : ordered) {
            if (migration.getVersion() <= currentVersion) continue;
            try {
                this.database.transaction(connection -> {
                    migration.migrate(connection, this.tablePrefix);
                    recordMigration(connection, migration.getVersion());
                });
            } catch (RuntimeException exception) {
                throw new DatabaseException("Database migration " + migration.getVersion() + " failed", exception);
            }
            currentVersion = migration.getVersion();
            applied.add(currentVersion);
        }

        return new MigrationResult(previousVersion, currentVersion, applied);
    }

    int readCurrentVersion() {
        return this.database.withConnectionResult(connection -> {
            ensureHistoryTable(connection);
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT MAX(version) FROM " + this.historyTable)) {
                return result.next() ? result.getInt(1) : 0;
            }
        });
    }

    private void recordMigration(Connection connection, int version) throws Exception {
        ensureHistoryTable(connection);
        String sql = "INSERT INTO " + this.historyTable + " (version, applied_at) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, version);
            statement.setLong(2, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void ensureHistoryTable(Connection connection) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS " + this.historyTable
                + " (version INT NOT NULL PRIMARY KEY, applied_at BIGINT NOT NULL)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static List<DatabaseMigration> validateAndOrder(DatabaseMigration[] migrations) {
        if (migrations == null || migrations.length == 0) return new ArrayList<>();
        List<DatabaseMigration> ordered = new ArrayList<>(Arrays.asList(migrations));
        Set<Integer> versions = new HashSet<>();
        for (DatabaseMigration migration : ordered) {
            if (migration == null) throw new IllegalArgumentException("Migration cannot be null");
            if (!versions.add(migration.getVersion())) {
                throw new IllegalArgumentException("Duplicate migration version " + migration.getVersion());
            }
        }
        ordered.sort(Comparator.comparingInt(DatabaseMigration::getVersion));
        return ordered;
    }

    static String validatePrefix(String prefix) {
        String value = prefix == null ? "" : prefix.trim();
        if (!value.matches("[A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Table prefix may only contain letters, digits and underscores");
        }
        return value;
    }
}
