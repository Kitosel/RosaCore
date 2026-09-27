package pl.kiosel.rosacore.message;

import java.util.*;

public final class RosaBossBarManager implements AutoCloseable {

	private final RosaMessenger messenger;
	private final Set<RosaBossBar> bars = new LinkedHashSet<>();
	private boolean closed;

	public RosaBossBarManager(RosaMessenger messenger) {
		this.messenger = Objects.requireNonNull(messenger, "messenger");
	}

	public RosaBossBar create(String text) {
		return create(text, 1.0F);
	}

	public RosaBossBar create(String text, float progress) {
		return create(text, progress, RosaBossBar.Color.PURPLE, RosaBossBar.Style.SOLID);
	}

	public synchronized RosaBossBar create(String text, float progress,
										   RosaBossBar.Color color, RosaBossBar.Style style,
										   Object... placeholders) {
		ensureOpen();
		RosaBossBar bar = new RosaBossBar(this, this.messenger, text, progress,
				Objects.requireNonNull(color, "color"),
				Objects.requireNonNull(style, "style"), placeholders);
		this.bars.add(bar);
		return bar;
	}

	public synchronized int size() {
		return this.bars.size();
	}

	public synchronized List<RosaBossBar> getBars() {
		return Collections.unmodifiableList(new ArrayList<>(this.bars));
	}

	public synchronized boolean isClosed() {
		return this.closed;
	}

	@Override
	public void close() {
		List<RosaBossBar> snapshot;
		synchronized (this) {
			if (this.closed) return;
			this.closed = true;
			snapshot = new ArrayList<>(this.bars);
			this.bars.clear();
		}
		RuntimeException failure = null;
		for (RosaBossBar bar : snapshot) {
			try {
				bar.close();
			} catch (RuntimeException exception) {
				if (failure == null) failure = exception;
				else failure.addSuppressed(exception);
			}
		}
		if (failure != null) throw failure;
	}

	synchronized void forget(RosaBossBar bar) {
		this.bars.remove(bar);
	}

	private synchronized void ensureOpen() {
		if (this.closed) throw new IllegalStateException("Boss bar manager is closed");
	}
}
