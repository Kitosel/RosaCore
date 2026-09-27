package pl.kiosel.rosacore.database;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class MigrationResult {

	private final int previousVersion;
	private final int currentVersion;
	private final List<Integer> appliedVersions;

	MigrationResult(int previousVersion, int currentVersion, List<Integer> appliedVersions) {
		this.previousVersion = previousVersion;
		this.currentVersion = currentVersion;
		this.appliedVersions = Collections.unmodifiableList(new ArrayList<>(appliedVersions));
	}

	public boolean changed() {
		return !this.appliedVersions.isEmpty();
	}
}
