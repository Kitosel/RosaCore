package pl.kiosel.rosacore.config;

import lombok.Getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class ConfigLoadResult {

	public enum Status {
		LOADED,
		MIGRATED,
		FAILED
	}

	private final Status status;
	private final int previousVersion;
	private final int currentVersion;
	private final Path backupFile;
	private final List<ConfigProblem> problems;
	private final Throwable cause;

	private ConfigLoadResult(Status status, int previousVersion, int currentVersion, Path backupFile,
							 List<ConfigProblem> problems, Throwable cause) {
		this.status = status;
		this.previousVersion = previousVersion;
		this.currentVersion = currentVersion;
		this.backupFile = backupFile;
		this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
		this.cause = cause;
	}

	static ConfigLoadResult success(int previousVersion, int currentVersion, Path backupFile) {
		Status status = previousVersion == currentVersion ? Status.LOADED : Status.MIGRATED;
		return new ConfigLoadResult(status, previousVersion, currentVersion, backupFile,
				Collections.emptyList(), null);
	}

	static ConfigLoadResult failure(int previousVersion, int currentVersion,
									List<ConfigProblem> problems, Throwable cause) {
		return new ConfigLoadResult(Status.FAILED, previousVersion, currentVersion, null, problems, cause);
	}

	public static ConfigLoadResult loaded() {
		return success(0, 0, null);
	}

	public static ConfigLoadResult failed(String path, String message, Throwable cause) {
		return failure(0, 0, Collections.singletonList(new ConfigProblem(path, message)), cause);
	}

	public boolean isSuccess() {
		return this.status != Status.FAILED;
	}

	public boolean isMigrated() {
		return this.status == Status.MIGRATED;
	}

}
