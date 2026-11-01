package pl.kiosel.rosacore.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ConfigSet {

	private final RosaConfig config;
	private final String path;
	private final Object fallback;

	public ConfigSet(RosaConfig config, String path, Object fallback) {
		this.config = Objects.requireNonNull(config, "config");
		this.path = Objects.requireNonNull(path, "path");
		this.fallback = fallback;
	}

	public Object get() {
		return this.config.get(this.path, this.fallback);
	}

	public boolean getBoolean() {
		return this.config.getBoolean(this.path, this.fallback instanceof Boolean && (Boolean) this.fallback);
	}

	public int getInt() {
		return this.config.getInt(this.path, this.fallback instanceof Number ? ((Number) this.fallback).intValue() : 0);
	}

	public long getLong() {
		return this.config.getLong(this.path, this.fallback instanceof Number ? ((Number) this.fallback).longValue() : 0L);
	}

	public double getDouble() {
		return this.config.getDouble(this.path, this.fallback instanceof Number ? ((Number) this.fallback).doubleValue() : 0D);
	}

	public String getString() {
		return this.config.getString(this.path, this.fallback == null ? "" : String.valueOf(this.fallback));
	}

	public List<String> getStringList() {
		List<String> configured = this.config.getStringList(this.path);
		if (!configured.isEmpty()) return configured;
		if (!(this.fallback instanceof Iterable<?>)) return Collections.emptyList();

		List<String> values = new ArrayList<>();
		for (Object value : (Iterable<?>) this.fallback) values.add(String.valueOf(value));
		return values;
	}
}
