package pl.kiosel.rosacore.nms.api.server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ServerTps {

	private ServerTps() {
	}

	public static double[] read(Object bukkitServer, Object minecraftServer) {
		double[] values = invokeTps(bukkitServer);
		if (values != null) {
			return values;
		}

		Object spigot = invoke(bukkitServer, "spigot");
		values = invokeTps(spigot);
		if (values != null) {
			return values;
		}

		if (minecraftServer != null) {
			try {
				Field field = minecraftServer.getClass().getField("recentTps");
				Object value = field.get(minecraftServer);
				if (value instanceof double[] && ((double[]) value).length > 0) {
					return (double[]) value;
				}
			} catch (ReflectiveOperationException | SecurityException ignored) {
			}
		}

		throw new IllegalStateException("Server TPS is unavailable via the API and legacy NMS field");
	}

	private static double[] invokeTps(Object target) {
		Object value = invoke(target, "getTPS");
		return value instanceof double[] && ((double[]) value).length > 0
				? (double[]) value
				: null;
	}

	private static Object invoke(Object target, String name) {
		if (target == null) {
			return null;
		}
		try {
			Method method = target.getClass().getMethod(name);
			return method.invoke(target);
		} catch (ReflectiveOperationException | SecurityException ignored) {
			return null;
		}
	}
}
