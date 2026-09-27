package pl.kiosel.rosacore.nms.api.status;

public final class ServerStatusInterceptors {

	private ServerStatusInterceptors() {
	}

	public static ServerStatusInterceptor shared() {
		return Holder.INSTANCE;
	}

	private static final class Holder {
		private static final ServerStatusInterceptor INSTANCE = new ReflectiveServerStatusInterceptor();
	}
}
