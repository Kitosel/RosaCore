package pl.kiosel.rosacore.nms.api.status;

public interface ServerStatusInterceptor extends AutoCloseable {

	Subscription subscribe(ServerStatusPacketListener listener);

	void unsubscribe(ServerStatusPacketListener listener);

	int size();

	void clear();

	@Override
	default void close() {
		clear();
	}

	interface Subscription extends AutoCloseable {

		ServerStatusPacketListener getListener();

		boolean isClosed();

		@Override
		void close();
	}
}
