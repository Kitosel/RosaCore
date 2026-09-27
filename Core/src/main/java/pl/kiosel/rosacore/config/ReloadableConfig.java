package pl.kiosel.rosacore.config;

import java.nio.file.Path;

public interface ReloadableConfig {

	ConfigLoadResult reload();

	boolean isLoaded();

	Path getPath();
}
