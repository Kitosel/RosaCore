package pl.kiosel.rosacore.database;

@FunctionalInterface
public interface DatabaseRowConsumer {

	void accept(DatabaseRow row) throws Exception;
}
