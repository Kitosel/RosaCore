package pl.kiosel.rosacore.hook.internal;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.*;

public final class HookReflection {

	private HookReflection() {
	}

	public static Class<?> findClass(Plugin plugin, String name) throws ClassNotFoundException {
		if (plugin == null) throw new ClassNotFoundException(name + " (plugin is unavailable)");
		return Class.forName(name, true, plugin.getClass().getClassLoader());
	}

	public static Object invoke(Object target, String name, Object... arguments)
			throws ReflectiveOperationException {
		if (target == null) throw new NullPointerException("target");
		Method method = findCompatibleMethod(target.getClass(), name, arguments);
		return invokeMethod(method, target, arguments);
	}

	public static Object invokeStatic(Class<?> type, String name, Object... arguments)
			throws ReflectiveOperationException {
		Method method = findCompatibleMethod(type, name, arguments);
		if (!Modifier.isStatic(method.getModifiers()))
			throw new NoSuchMethodException(type.getName() + "." + name + " is not static");
		return invokeMethod(method, null, arguments);
	}

	public static Object invokeExact(Object target, String name, Class<?>[] parameterTypes, Object... arguments)
			throws ReflectiveOperationException {
		if (target == null) throw new NullPointerException("target");
		Method method = findMethod(target.getClass(), name, parameterTypes);
		return invokeMethod(method, target, arguments);
	}

	public static Object invokeStaticExact(Class<?> type, String name, Class<?>[] parameterTypes,
										   Object... arguments) throws ReflectiveOperationException {
		Method method = findMethod(type, name, parameterTypes);
		if (!Modifier.isStatic(method.getModifiers()))
			throw new NoSuchMethodException(type.getName() + "." + name + " is not static");
		return invokeMethod(method, null, arguments);
	}

	public static Object newInstance(Class<?> type, Object... arguments) throws ReflectiveOperationException {
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			Class<?>[] types = constructor.getParameterTypes();
			if (types.length != arguments.length) continue;

			boolean compatible = true;
			for (int index = 0; index < types.length; index++) {
				if (!isCompatible(types[index], arguments[index])) {
					compatible = false;
					break;
				}
			}
			if (!compatible) continue;
			constructor.setAccessible(true);
			try {
				return constructor.newInstance(arguments);
			} catch (InvocationTargetException exception) {
				Throwable cause = exception.getCause();
				if (cause instanceof ReflectiveOperationException)
					throw (ReflectiveOperationException) cause;
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;
				if (cause instanceof Error) throw (Error) cause;
				throw exception;
			}
		}
		throw new NoSuchMethodException("Compatible constructor for " + type.getName());
	}

	public static Object readField(Object target, String name) throws ReflectiveOperationException {
		if (target == null) throw new NullPointerException("target");
		Field field = findField(target.getClass(), name);
		return field.get(target);
	}

	public static Object readStaticField(Class<?> type, String name) throws ReflectiveOperationException {
		Field field = findField(type, name);
		return field.get(null);
	}

	public static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes)
			throws NoSuchMethodException {
		Class<?> current = type;
		while (current != null) {
			try {
				Method method = current.getDeclaredMethod(name, parameterTypes);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException ignored) {
				current = current.getSuperclass();
			}
		}
		return type.getMethod(name, parameterTypes);
	}

	public static Method findCompatibleMethod(Class<?> type, String name, Object... arguments)
			throws NoSuchMethodException {
		Method method = findCompatible(type.getMethods(), name, arguments);
		if (method != null) {
			method.setAccessible(true);
			return method;
		}

		Class<?> current = type;
		while (current != null) {
			method = findCompatible(current.getDeclaredMethods(), name, arguments);
			if (method != null) {
				method.setAccessible(true);
				return method;
			}
			current = current.getSuperclass();
		}
		throw new NoSuchMethodException(type.getName() + "." + name);
	}

	private static Method findCompatible(Method[] methods, String name, Object[] arguments) {
		for (Method method : methods) {
			if (!method.getName().equals(name)) continue;
			Class<?>[] types = method.getParameterTypes();
			if (types.length != arguments.length) continue;

			boolean compatible = true;
			for (int index = 0; index < types.length; index++) {
				if (!isCompatible(types[index], arguments[index])) {
					compatible = false;
					break;
				}
			}
			if (compatible) return method;
		}
		return null;
	}

	private static boolean isCompatible(Class<?> parameterType, Object argument) {
		if (argument == null) return !parameterType.isPrimitive();
		Class<?> actualType = argument.getClass();
		if (parameterType.isAssignableFrom(actualType)) return true;
		if (!parameterType.isPrimitive()) return false;
		if (argument instanceof Byte)
			return parameterType == byte.class || parameterType == short.class || parameterType == int.class
					|| parameterType == long.class || parameterType == float.class || parameterType == double.class;
		if (argument instanceof Short)
			return parameterType == short.class || parameterType == int.class || parameterType == long.class
					|| parameterType == float.class || parameterType == double.class;
		if (argument instanceof Integer)
			return parameterType == int.class || parameterType == long.class
					|| parameterType == float.class || parameterType == double.class;
		if (argument instanceof Long)
			return parameterType == long.class || parameterType == float.class || parameterType == double.class;
		if (argument instanceof Float)
			return parameterType == float.class || parameterType == double.class;
		if (argument instanceof Double) return parameterType == double.class;
		return (parameterType == boolean.class && actualType == Boolean.class)
				|| (parameterType == byte.class && actualType == Byte.class)
				|| (parameterType == short.class && actualType == Short.class)
				|| (parameterType == int.class && actualType == Integer.class)
				|| (parameterType == long.class && actualType == Long.class)
				|| (parameterType == float.class && actualType == Float.class)
				|| (parameterType == double.class && actualType == Double.class)
				|| (parameterType == char.class && actualType == Character.class);
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				Field field = current.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(type.getName() + "." + name);
	}

	private static Object invokeMethod(Method method, Object target, Object[] arguments)
			throws ReflectiveOperationException {
		try {
			return method.invoke(target, arguments);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof ReflectiveOperationException)
				throw (ReflectiveOperationException) cause;
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			if (cause instanceof Error) throw (Error) cause;
			throw exception;
		}
	}
}
