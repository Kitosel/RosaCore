package pl.kiosel.rosacore.nms.api.packet;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class TabOutboundPacketEvent {

	private final Player viewer;
	private final boolean rosaCorePacket;
	private final List<Runnable> afterSendTasks = new ArrayList<>();
	private boolean cancelled;

	protected TabOutboundPacketEvent(Player viewer, boolean rosaCorePacket) {
		if (viewer == null) throw new NullPointerException("viewer");
		this.viewer = viewer;
		this.rosaCorePacket = rosaCorePacket;
	}

	public final Player getViewer() {
		return viewer;
	}

	public final boolean isRosaCorePacket() {
		return rosaCorePacket;
	}

	public final boolean isCancelled() {
		return cancelled;
	}

	public final void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public final void afterSend(Runnable task) {
		if (task != null) afterSendTasks.add(task);
	}

	final void runAfterSendTasks() {
		for (Runnable task : afterSendTasks) task.run();
		afterSendTasks.clear();
	}
}
