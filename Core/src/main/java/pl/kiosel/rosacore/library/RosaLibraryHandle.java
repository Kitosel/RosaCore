package pl.kiosel.rosacore.library;

import lombok.Getter;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;

public final class RosaLibraryHandle implements AutoCloseable {

	@Getter
	private final RosaLibrary library;
	@Getter
	private final Path file;
	private final URLClassLoader classLoader;

	RosaLibraryHandle(RosaLibrary library, Path file, URLClassLoader classLoader) {
		this.library = Objects.requireNonNull(library, "library");
		this.file = Objects.requireNonNull(file, "file");
		this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
	}

	public Class<?> loadClass(String className) {
		try {
			return Class.forName(Objects.requireNonNull(className, "className"), true, this.classLoader);
		} catch (ClassNotFoundException exception) {
			throw new LibraryLoadException("Class " + className + " is missing from " + this.library, exception);
		}
	}

	@Override
	public void close() {
		try {
			this.classLoader.close();
		} catch (IOException exception) {
			throw new LibraryLoadException("Could not close " + this.library + " library", exception);
		}
	}
}
