package pl.kiosel.rosacore.config;

public interface MutableConfig extends ConfigView {

	void set(String path, Object value);

	void remove(String path);
}
