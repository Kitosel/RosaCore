package pl.kiosel.rosacore.config;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class ConfigProblem {

	private final String path;
	private final String message;

	public ConfigProblem(String path, String message) {
		this.path = path == null ? "" : path;
		this.message = Objects.requireNonNull(message, "message");
	}

	@Override
	public String toString() {
		return this.path.isEmpty() ? this.message : this.path + ": " + this.message;
	}
}
