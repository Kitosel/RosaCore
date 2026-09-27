package pl.kiosel.rosacore.nms.api.packet;

import org.bukkit.entity.Player;

import java.util.*;

public final class PlayerInfoPacketEvent extends TabOutboundPacketEvent {

	private final Set<TabPacketAction> actions;
	private final List<TabPacketEntry> entries;

	public PlayerInfoPacketEvent(Player viewer, boolean rosaCorePacket,
								 Collection<TabPacketAction> actions,
								 Collection<TabPacketEntry> entries) {
		super(viewer, rosaCorePacket);
		EnumSet<TabPacketAction> copiedActions = actions == null || actions.isEmpty()
				? EnumSet.of(TabPacketAction.UNKNOWN)
				: EnumSet.copyOf(actions);
		this.actions = Collections.unmodifiableSet(copiedActions);
		this.entries = Collections.unmodifiableList(entries == null
				? Collections.emptyList()
				: new ArrayList<>(entries));
	}

	public Set<TabPacketAction> getActions() {
		return actions;
	}

	public boolean hasAction(TabPacketAction action) {
		return actions.contains(action);
	}

	public List<TabPacketEntry> getEntries() {
		return entries;
	}
}
