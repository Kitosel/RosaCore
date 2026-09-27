package pl.kiosel.rosacore.nms.api.tablist;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class AuthlibProfileAccess {

	private AuthlibProfileAccess() {
	}

	static Object properties(Object profile) throws ReflectiveOperationException {
		if (profile == null) throw new NullPointerException("profile");

		for (String name : new String[]{"getProperties", "properties"}) {
			Object value = invokeNoArgs(profile, name);
			if (value != null) return value;
		}

		for (Method method : methods(profile.getClass())) {
			if (Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 0
					|| !isPropertyMap(method.getReturnType())) continue;
			Object value = invoke(method, profile);
			if (value != null) return value;
		}

		for (Field field : fields(profile.getClass())) {
			if (Modifier.isStatic(field.getModifiers()) || !isPropertyMap(field.getType())) continue;
			Object value = read(field, profile);
			if (value != null) return value;
		}

		throw new NoSuchMethodException("PropertyMap in " + profile.getClass().getName());
	}

	static Object createProfile(Class<?> profileType, Constructor<?> basicConstructor,
								UUID id, String name, Object textureProperty) throws ReflectiveOperationException {
		if (textureProperty != null) {
			Object modernProfile = createProfileWithProperties(profileType, id, name, textureProperty);
			if (modernProfile != null) return modernProfile;
		}

		Object profile = basicConstructor.newInstance(id, name);
		if (textureProperty != null) put(properties(profile), "textures", textureProperty);
		return profile;
	}

	private static Object createProfileWithProperties(Class<?> profileType, UUID id, String name,
													  Object textureProperty) {
		for (Constructor<?> constructor : profileType.getDeclaredConstructors()) {
			Class<?>[] parameters = constructor.getParameterTypes();
			if (parameters.length != 3 || parameters[0] != UUID.class || parameters[1] != String.class
					|| !isPropertyMap(parameters[2])) continue;
			Object propertyMap = createPropertyMap(parameters[2], textureProperty);
			if (propertyMap == null) continue;
			try {
				constructor.setAccessible(true);
				return constructor.newInstance(id, name, propertyMap);
			} catch (ReflectiveOperationException | RuntimeException ignored) {
			}
		}
		return null;
	}

	private static Object createPropertyMap(Class<?> propertyMapType, Object textureProperty) {
		try {
			ClassLoader loader = propertyMapType.getClassLoader();
			Class<?> immutableMultimap = Class.forName("com.google.common.collect.ImmutableMultimap", true, loader);
			Object values = null;
			for (Method method : immutableMultimap.getMethods()) {
				if (!method.getName().equals("of") || !Modifier.isStatic(method.getModifiers())
						|| method.getParameterTypes().length != 2) continue;
				values = method.invoke(null, "textures", textureProperty);
				break;
			}
			if (values == null) return null;

			for (Constructor<?> constructor : propertyMapType.getDeclaredConstructors()) {
				Class<?>[] parameters = constructor.getParameterTypes();
				if (parameters.length != 1 || !parameters[0].isInstance(values)) continue;
				constructor.setAccessible(true);
				return constructor.newInstance(values);
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
		return null;
	}

	private static void put(Object properties, String key, Object property)
			throws ReflectiveOperationException {
		for (Method method : properties.getClass().getMethods()) {
			if (!method.getName().equals("put") || method.getParameterTypes().length != 2) continue;
			method.invoke(properties, key, property);
			return;
		}
		throw new NoSuchMethodException("No property-map put method in " + properties.getClass().getName());
	}

	private static Object invokeNoArgs(Object owner, String name) {
		for (Method method : methods(owner.getClass())) {
			if (!method.getName().equals(name) || method.getParameterTypes().length != 0) continue;
			Object value = invoke(method, owner);
			if (value != null) return value;
		}
		return null;
	}

	private static Object invoke(Method method, Object owner) {
		try {
			method.setAccessible(true);
			return method.invoke(owner);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static Object read(Field field, Object owner) {
		try {
			field.setAccessible(true);
			return field.get(owner);
		} catch (IllegalAccessException | RuntimeException ignored) {
			return null;
		}
	}

	private static boolean isPropertyMap(Class<?> type) {
		return "PropertyMap".equals(type.getSimpleName())
				|| type.getName().endsWith(".properties.PropertyMap");
	}

	private static List<Method> methods(Class<?> type) {
		List<Method> result = new ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			Collections.addAll(result, current.getDeclaredMethods());
			current = current.getSuperclass();
		}
		return result;
	}

	private static List<Field> fields(Class<?> type) {
		List<Field> result = new ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			Collections.addAll(result, current.getDeclaredFields());
			current = current.getSuperclass();
		}
		return result;
	}
}
