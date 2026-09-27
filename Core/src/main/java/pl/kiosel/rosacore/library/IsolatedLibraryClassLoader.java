package pl.kiosel.rosacore.library;

import java.net.URL;
import java.net.URLClassLoader;

final class IsolatedLibraryClassLoader extends URLClassLoader {

	IsolatedLibraryClassLoader(URL library, ClassLoader parent) {
		super(new URL[]{library}, parent);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> loaded = findLoadedClass(name);
			if (loaded == null) {
				if (isJvmClass(name)) {
					loaded = super.loadClass(name, false);
				} else {
					try {
						loaded = findClass(name);
					} catch (ClassNotFoundException ignored) {
						loaded = super.loadClass(name, false);
					}
				}
			}
			if (resolve) resolveClass(loaded);
			return loaded;
		}
	}

	private static boolean isJvmClass(String name) {
		return name.startsWith("java.")
				|| name.startsWith("javax.")
				|| name.startsWith("jdk.")
				|| name.startsWith("sun.");
	}
}
