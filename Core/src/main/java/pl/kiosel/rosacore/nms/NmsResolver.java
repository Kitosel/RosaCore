package pl.kiosel.rosacore.nms;

import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.nms.api.NMS;

import java.util.Objects;
import java.util.regex.Pattern;

public final class NmsResolver {

	private static final Pattern REVISION = Pattern.compile("^v\\d+_\\d+_R\\d+$");

	private NmsResolver() {
	}

	public static NMS resolve(RosaPlugin plugin, String revision, ClassLoader classLoader) {
		Objects.requireNonNull(classLoader, "classLoader");
		String className = implementationClassName(revision);

		plugin.getDebug().debug("Server Revision " + revision);
		plugin.getDebug().debug("NmsResolver class name: " + className);

		try {
			Class<?> rawType = Class.forName(className, true, classLoader);
			Class<? extends NMS> implementationType = rawType.asSubclass(NMS.class);
			return implementationType.getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException exception) {
			throw new UnsupportedOperationException(
					"RosaCore does not contain an NMS implementation for " + revision, exception);
		} catch (ReflectiveOperationException | ClassCastException exception) {
			throw new IllegalStateException("Could not create NMS implementation " + className, exception);
		} catch (LinkageError error) {
			throw new IllegalStateException(
					"NMS implementation " + className + " is not compatible with this server", error);
		}
	}

	private static String implementationClassName(String revision) {
		if (revision == null || !REVISION.matcher(revision).matches()) {
			throw new UnsupportedOperationException("Unknown server revision: " + revision);
		}
		String rev = revision;
		if (rev.startsWith("v26") || rev.startsWith("v27")) {
			rev = "v26";
		}
		return implementationPackage() + rev + ".NmsImpl";
	}

	private static String implementationPackage() {
		String apiPackage = NMS.class.getPackage().getName();
		if (!apiPackage.endsWith(".api")) {
			throw new IllegalStateException("Unexpected relocated NMS API package: " + apiPackage);
		}
		return apiPackage.substring(0, apiPackage.length() - ".api".length()) + ".";
	}
}
