package pl.kiosel.rosacore.nms.api.packet;

public final class TabPacketInterceptors {

	private TabPacketInterceptors() {
	}

	public static TabPacketInterceptor shared() {
		return Holder.INSTANCE;
	}

	private static final class Holder {
		private static final TabPacketInterceptor INSTANCE = new ReflectiveTabPacketInterceptor();
	}
}
