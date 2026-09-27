package pl.kiosel.rosacore.config;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class ConfigSaveResult {

	private final boolean success;
	private final List<ConfigProblem> problems;
	private final Throwable cause;

	private ConfigSaveResult(boolean success, List<ConfigProblem> problems, Throwable cause) {
		this.success = success;
		this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
		this.cause = cause;
	}

	public static ConfigSaveResult success() {
		return new ConfigSaveResult(true, Collections.emptyList(), null);
	}

	static ConfigSaveResult failure(List<ConfigProblem> problems, Throwable cause) {
		return new ConfigSaveResult(false, problems, cause);
	}

}
