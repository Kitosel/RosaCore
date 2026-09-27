package pl.kiosel.rosacore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class RosaCoreBuildInfo {

	private static final String RESOURCE = "META-INF/rosacore/version.properties";
	private static final String VERSION = loadVersion();

	private RosaCoreBuildInfo() {
	}

	static String getVersion() {
		return VERSION;
	}

	private static String loadVersion() {
		Properties properties = new Properties();
		ClassLoader classLoader = RosaCoreBuildInfo.class.getClassLoader();
		try (InputStream input = classLoader.getResourceAsStream(RESOURCE)) {
			if (input == null)
				return "DEV";

			properties.load(input);
			String version = properties.getProperty("version");
			return version == null || version.trim().isEmpty() ? "DEV" : version.trim();
		} catch (IOException ignored) {
			return "DEV";
		}
	}
}
