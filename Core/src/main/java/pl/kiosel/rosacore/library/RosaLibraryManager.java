package pl.kiosel.rosacore.library;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class RosaLibraryManager {

	public static final String REPOSITORY_PROPERTY = "rosacore.libraryRepository";
	public static final String DEFAULT_REPOSITORY = "https://repo.papermc.io/repository/maven-public";

	private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
	private static final int READ_TIMEOUT_MILLIS = 60_000;

	@Getter
	private final Path librariesDirectory;
	private final ClassLoader parentClassLoader;
	private final String repository;
	private final Logger logger;

	public RosaLibraryManager(Plugin plugin) {
		this(getLibrariesDirectory(Objects.requireNonNull(plugin, "plugin")),
				plugin.getClass().getClassLoader(),
				System.getProperty(REPOSITORY_PROPERTY, DEFAULT_REPOSITORY),
				plugin.getLogger()
		);
	}

	RosaLibraryManager(Path librariesDirectory, ClassLoader parentClassLoader, String repository) {
		this(librariesDirectory, parentClassLoader, repository, null);
	}

	private RosaLibraryManager(Path librariesDirectory, ClassLoader parentClassLoader, String repository, Logger logger) {
		this.librariesDirectory = Objects.requireNonNull(librariesDirectory, "librariesDirectory");
		this.parentClassLoader = Objects.requireNonNull(parentClassLoader, "parentClassLoader");
		this.repository = trimTrailingSlash(Objects.requireNonNull(repository, "repository"));
		this.logger = logger;
	}

	public RosaLibraryHandle load(RosaLibrary library) {
		Objects.requireNonNull(library, "library");
		try {
			Path file = resolveLibraryFile(library);
			ensureVerifiedLibrary(library, file);
			IsolatedLibraryClassLoader classLoader = new IsolatedLibraryClassLoader(
					file.toUri().toURL(), this.parentClassLoader);
			return new RosaLibraryHandle(library, file, classLoader);
		} catch (IOException exception) {
			throw new LibraryLoadException("Could not load runtime library " + library, exception);
		}
	}

	public Path resolveLibraryFile(RosaLibrary library) {
		Objects.requireNonNull(library, "library");
		return this.librariesDirectory
				.resolve(library.getGroupId().replace('.', '/'))
				.resolve(library.getArtifactId())
				.resolve(library.getVersion())
				.resolve(library.getFileName());
	}

	public static Path getLibrariesDirectory(Plugin plugin) {
		Path pluginsDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize().getParent();
		if (pluginsDirectory == null) {
			throw new IllegalArgumentException("Plugin data folder has no parent directory");
		}
		return pluginsDirectory.resolve("Rosa").resolve("libraries");
	}

	private void ensureVerifiedLibrary(RosaLibrary library, Path file) throws IOException {
		if (Files.isRegularFile(file) && hasExpectedChecksum(file, library.getSha256())) {
			return;
		}

		Files.createDirectories(file.getParent());
		Path temporary = file.resolveSibling(file.getFileName() + ".part-" + UUID.randomUUID());
		try {
			download(library, temporary);
			if (!hasExpectedChecksum(temporary, library.getSha256())) {
				throw new LibraryLoadException("SHA-256 verification failed for " + library);
			}

			if (Files.isRegularFile(file) && hasExpectedChecksum(file, library.getSha256())) {
				return;
			}
			Files.deleteIfExists(file);
			moveWithoutReplacing(temporary, file);
			if (!hasExpectedChecksum(file, library.getSha256())) {
				throw new LibraryLoadException("Cached library failed SHA-256 verification: " + file);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private void download(RosaLibrary library, Path destination) throws IOException {
		URL url = new URL(this.repository + '/' + library.getRepositoryPath());
		if (this.logger != null) {
			this.logger.info("[RosaCore] Downloading " + library.getArtifactId() + ' ' + library.getVersion());
		}
		URLConnection connection = getConnection(url);

		try (InputStream input = connection.getInputStream()) {
			Files.copy(input, destination);
		} finally {
			if (connection instanceof HttpURLConnection) {
				((HttpURLConnection) connection).disconnect();
			}
		}
	}

	private static @NonNull URLConnection getConnection(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
		connection.setReadTimeout(READ_TIMEOUT_MILLIS);
		connection.setUseCaches(false);
		connection.setRequestProperty("User-Agent", "RosaCore-LibraryLoader/1");

		if (connection instanceof HttpURLConnection) {
			HttpURLConnection http = (HttpURLConnection) connection;
			int status = http.getResponseCode();
			if (status < 200 || status >= 300) {
				http.disconnect();
				throw new IOException("Library repository returned HTTP " + status + " for " + url);
			}
		}
		return connection;
	}

	private static void moveWithoutReplacing(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (FileAlreadyExistsException ignored) {
		} catch (AtomicMoveNotSupportedException ignored) {
			try {
				Files.move(source, target);
			} catch (FileAlreadyExistsException ignored1) {
			}
		}
	}

	private static boolean hasExpectedChecksum(Path file, String expected) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}

		try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
			byte[] buffer = new byte[8192];
			while (input.read(buffer) != -1) {
			}
		}
		return toHex(digest.digest()).equals(expected.toLowerCase(Locale.ROOT));
	}

	private static String toHex(byte[] bytes) {
		char[] alphabet = "0123456789abcdef".toCharArray();
		StringBuilder output = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			int unsigned = value & 0xff;
			output.append(alphabet[unsigned >>> 4]);
			output.append(alphabet[unsigned & 0x0f]);
		}
		return output.toString();
	}

	private static String trimTrailingSlash(String value) {
		String result = value.trim();
		while (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		if (result.isEmpty()) {
			throw new IllegalArgumentException("Library repository cannot be empty");
		}
		return result;
	}
}
