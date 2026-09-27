package pl.kiosel.rosacore.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConfigView {

	Object get(String path);

	Object get(String path, Object fallback);

	boolean contains(String path);

	boolean isSet(String path);

	boolean isLocallySet(String path);

	String getString(String path);

	String getString(String path, String fallback);

	boolean getBoolean(String path);

	boolean getBoolean(String path, boolean fallback);

	int getInt(String path);

	int getInt(String path, int fallback);

	long getLong(String path);

	long getLong(String path, long fallback);

	double getDouble(String path);

	double getDouble(String path, double fallback);

	List<String> getStringList(String path);

	ConfigurationSection getConfigurationSection(String path);

	Set<String> getKeys(boolean deep);

	Map<String, Object> getValues(boolean deep);
}
