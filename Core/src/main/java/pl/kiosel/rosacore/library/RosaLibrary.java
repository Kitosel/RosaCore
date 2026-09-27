package pl.kiosel.rosacore.library;

import lombok.Getter;

@Getter
public enum RosaLibrary {

	H2(
			"com.h2database",
			"h2",
			"1.4.200",
			"3ad9ac4b6aae9cd9d3ac1c447465e1ed06019b851b893dd6a8d76ddb6d85bca6"
	),
	MARIADB(
			"org.mariadb.jdbc",
			"mariadb-java-client",
			"3.5.9",
			"11e3bb5bbf8ef0e806ae4d6c5d5033fedf7262cc777f0190bde8a2f3c8e6bd8d"
	);

	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String sha256;

	RosaLibrary(String groupId, String artifactId, String version, String sha256) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.sha256 = sha256;
	}

	public String getFileName() {
		return this.artifactId + "-" + this.version + ".jar";
	}

	String getRepositoryPath() {
		return this.groupId.replace('.', '/') + '/'
				+ this.artifactId + '/'
				+ this.version + '/'
				+ getFileName();
	}
}
