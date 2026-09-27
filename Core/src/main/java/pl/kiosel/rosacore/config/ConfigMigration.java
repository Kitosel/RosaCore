package pl.kiosel.rosacore.config;

@FunctionalInterface
public interface ConfigMigration {

	void migrate(MutableConfig config) throws Exception;
}
