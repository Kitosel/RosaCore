package pl.kiosel.rosacore.nms.api.packet;

import org.bukkit.entity.Player;

public interface TabPacketInterceptor extends AutoCloseable {

	Subscription subscribe(Player viewer, TabPacketListener listener);

	void unsubscribe(Player viewer, TabPacketListener listener);

	void clear(Player viewer);

	void clearAll();

	boolean isInjected(Player viewer);

	int size();

	@Override
	default void close() {
		clearAll();
	}

	interface Subscription extends AutoCloseable {

		Player getViewer();

		TabPacketListener getListener();

		boolean isClosed();

		@Override
		void close();
	}
}
