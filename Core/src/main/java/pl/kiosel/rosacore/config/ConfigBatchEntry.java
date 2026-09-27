package pl.kiosel.rosacore.config;

import lombok.Getter;

import java.nio.file.Path;
import java.util.Objects;

@Getter
public final class ConfigBatchEntry {

	public enum Status {
		SUCCESS,
		FAILED,
		SKIPPED
	}

	private final String id;
	private final Path path;
	private final Status status;
	private final ConfigLoadResult loadResult;
	private final String skipReason;

	private ConfigBatchEntry(String id, Path path, Status status,
							 ConfigLoadResult loadResult, String skipReason) {
		this.id = Objects.requireNonNull(id, "id");
		this.path = Objects.requireNonNull(path, "path");
		this.status = Objects.requireNonNull(status, "status");
		this.loadResult = loadResult;
		this.skipReason = skipReason;
	}

	static ConfigBatchEntry completed(String id, Path path, ConfigLoadResult result) {
		Objects.requireNonNull(result, "result");
		return new ConfigBatchEntry(id, path,
				result.isSuccess() ? Status.SUCCESS : Status.FAILED, result, null);
	}

	static ConfigBatchEntry skipped(String id, Path path, String reason) {
		return new ConfigBatchEntry(id, path, Status.SKIPPED, null,
				Objects.requireNonNull(reason, "reason"));
	}

	public boolean isSuccess() {
		return this.status == Status.SUCCESS;
	}

}
