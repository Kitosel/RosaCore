package pl.kiosel.rosacore.config;

import lombok.Getter;

import java.util.*;

public final class ConfigBatchResult {

	@Getter
	private final List<ConfigBatchEntry> entries;
	private final Map<String, ConfigBatchEntry> entriesById;

	ConfigBatchResult(List<ConfigBatchEntry> entries) {
		this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
		Map<String, ConfigBatchEntry> byId = new LinkedHashMap<>();
		for (ConfigBatchEntry entry : entries) {
			byId.put(entry.getId(), entry);
		}
		this.entriesById = Collections.unmodifiableMap(byId);
	}

	public boolean isSuccess() {
		for (ConfigBatchEntry entry : this.entries) {
			if (!entry.isSuccess()) {
				return false;
			}
		}
		return true;
	}

	public int getSuccessCount() {
		return this.count(ConfigBatchEntry.Status.SUCCESS);
	}

	public int getFailureCount() {
		return this.count(ConfigBatchEntry.Status.FAILED);
	}

	public int getSkippedCount() {
		return this.count(ConfigBatchEntry.Status.SKIPPED);
	}

	public Optional<ConfigBatchEntry> get(String id) {
		return Optional.ofNullable(this.entriesById.get(id));
	}

	private int count(ConfigBatchEntry.Status status) {
		int count = 0;
		for (ConfigBatchEntry entry : this.entries) {
			if (entry.getStatus() == status) {
				count++;
			}
		}
		return count;
	}
}
