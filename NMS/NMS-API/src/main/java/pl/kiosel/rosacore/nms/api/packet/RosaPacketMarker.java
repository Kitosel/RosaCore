package pl.kiosel.rosacore.nms.api.packet;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class RosaPacketMarker {

	private static final int MAX_PENDING_PACKETS = 4096;
	private static final Map<Object, Boolean> PACKETS = Collections.synchronizedMap(new IdentityHashMap<>());

	private RosaPacketMarker() {
	}

	public static <T> T mark(T packet) {
		if (packet == null) return null;
		synchronized (PACKETS) {
			if (PACKETS.size() >= MAX_PENDING_PACKETS) PACKETS.clear();
			PACKETS.put(packet, Boolean.TRUE);
		}
		return packet;
	}

	static boolean claim(Object packet) {
		return packet != null && PACKETS.remove(packet) != null;
	}
}
