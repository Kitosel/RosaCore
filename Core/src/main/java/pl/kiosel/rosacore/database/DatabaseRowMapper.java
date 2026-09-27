package pl.kiosel.rosacore.database;

@FunctionalInterface
public interface DatabaseRowMapper<T> {

	T map(DatabaseRow row) throws Exception;
}
