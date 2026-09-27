package pl.kiosel.rosacore.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

final class YamlConfigView implements MutableConfig {

	private final ConfigurationSection section;

	YamlConfigView(ConfigurationSection section) {
		this.section = Objects.requireNonNull(section, "section");
	}

	@Override
	public Object get(String path) {
		return this.section.get(path);
	}

	@Override
	public Object get(String path, Object fallback) {
		return this.section.get(path, fallback);
	}

	@Override
	public boolean contains(String path) {
		return this.section.contains(path);
	}

	@Override
	public boolean isSet(String path) {
		return this.section.isSet(path);
	}

	@Override
	public boolean isLocallySet(String path) {
		return this.section.getValues(true).containsKey(path);
	}

	@Override
	public String getString(String path) {
		return this.section.getString(path);
	}

	@Override
	public String getString(String path, String fallback) {
		return this.section.getString(path, fallback);
	}

	@Override
	public boolean getBoolean(String path) {
		return this.section.getBoolean(path);
	}

	@Override
	public boolean getBoolean(String path, boolean fallback) {
		return this.section.getBoolean(path, fallback);
	}

	@Override
	public int getInt(String path) {
		return this.section.getInt(path);
	}

	@Override
	public int getInt(String path, int fallback) {
		return this.section.getInt(path, fallback);
	}

	@Override
	public long getLong(String path) {
		return this.section.getLong(path);
	}

	@Override
	public long getLong(String path, long fallback) {
		return this.section.getLong(path, fallback);
	}

	@Override
	public double getDouble(String path) {
		return this.section.getDouble(path);
	}

	@Override
	public double getDouble(String path, double fallback) {
		return this.section.getDouble(path, fallback);
	}

	@Override
	public List<String> getStringList(String path) {
		return new ArrayList<>(this.section.getStringList(path));
	}

	@Override
	public ConfigurationSection getConfigurationSection(String path) {
		return this.section.getConfigurationSection(path);
	}

	@Override
	public Set<String> getKeys(boolean deep) {
		return Collections.unmodifiableSet(new LinkedHashSet<>(this.section.getKeys(deep)));
	}

	@Override
	public Map<String, Object> getValues(boolean deep) {
		return Collections.unmodifiableMap(new LinkedHashMap<>(this.section.getValues(deep)));
	}

	@Override
	public void set(String path, Object value) {
		this.section.set(path, value);
	}

	@Override
	public void remove(String path) {
		this.section.set(path, null);
	}
}
