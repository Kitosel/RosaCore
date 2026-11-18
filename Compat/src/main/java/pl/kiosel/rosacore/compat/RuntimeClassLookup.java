package pl.kiosel.rosacore.compat;

public final class RuntimeClassLookup implements ClassLookup {

	private final ClassLoader classLoader;

	public RuntimeClassLookup(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public boolean isPresent(String className) {
		try {
			Class.forName(className, false, this.classLoader);
			return true;
		} catch (ClassNotFoundException | LinkageError ignored) {
			return false;
		}
	}
}
