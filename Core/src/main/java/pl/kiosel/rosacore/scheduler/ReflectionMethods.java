package pl.kiosel.rosacore.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

final class ReflectionMethods {

	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<>();

	static {
		PRIMITIVE_WRAPPERS.put(boolean.class, Boolean.class);
		PRIMITIVE_WRAPPERS.put(byte.class, Byte.class);
		PRIMITIVE_WRAPPERS.put(short.class, Short.class);
		PRIMITIVE_WRAPPERS.put(int.class, Integer.class);
		PRIMITIVE_WRAPPERS.put(long.class, Long.class);
		PRIMITIVE_WRAPPERS.put(float.class, Float.class);
		PRIMITIVE_WRAPPERS.put(double.class, Double.class);
		PRIMITIVE_WRAPPERS.put(char.class, Character.class);
	}

	private ReflectionMethods() {
	}

	static Object invoke(Object target, String methodName, Object... arguments) {
		if (target == null)
			throw new IllegalArgumentException("Reflection target cannot be null");

		Method method = findCompatibleMethod(target.getClass(), methodName, arguments);
		try {
			return method.invoke(target, arguments);
		} catch (IllegalAccessException exception) {
			throw new IllegalStateException("Cannot access " + describe(method), exception);
		} catch (InvocationTargetException exception) {
			throw propagate(method, exception.getCause());
		}
	}

	private static Method findCompatibleMethod(Class<?> type, String methodName, Object[] arguments) {
		for (Method method : type.getMethods()) {
			if (!method.getName().equals(methodName)
					|| method.getParameterTypes().length != arguments.length)
				continue;

			if (accepts(method.getParameterTypes(), arguments))
				return method;
		}

		throw new IllegalStateException(
				"No compatible " + methodName + " method with " + arguments.length
						+ " arguments on " + type.getName()
		);
	}

	private static boolean accepts(Class<?>[] parameterTypes, Object[] arguments) {
		for (int index = 0; index < parameterTypes.length; index++) {
			Object argument = arguments[index];
			Class<?> parameterType = wrap(parameterTypes[index]);

			if (argument == null) {
				if (parameterTypes[index].isPrimitive())
					return false;
				continue;
			}

			if (!parameterType.isAssignableFrom(argument.getClass()))
				return false;
		}
		return true;
	}

	private static Class<?> wrap(Class<?> type) {
		Class<?> wrapper = PRIMITIVE_WRAPPERS.get(type);
		return wrapper == null ? type : wrapper;
	}

	private static RuntimeException propagate(Method method, Throwable throwable) {
		if (throwable instanceof RuntimeException)
			return (RuntimeException) throwable;
		if (throwable instanceof Error)
			throw (Error) throwable;

		return new IllegalStateException("Invocation failed for " + describe(method), throwable);
	}

	private static String describe(Method method) {
		return method.getDeclaringClass().getName() + "#" + method.getName();
	}
}
