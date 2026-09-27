package pl.kiosel.rosacore.nms.api.status;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ServerStatusPacketEvent {

	private final SocketAddress remoteAddress;
	private int onlinePlayers;
	private int maxPlayers;
	private final List<ServerStatusSample> sample = new ArrayList<>();

	public ServerStatusPacketEvent(SocketAddress remoteAddress, int onlinePlayers, int maxPlayers,
								   Collection<ServerStatusSample> sample) {
		this.remoteAddress = remoteAddress;
		setOnlinePlayers(onlinePlayers);
		setMaxPlayers(maxPlayers);
		setSample(sample);
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public int getOnlinePlayers() {
		return onlinePlayers;
	}

	public void setOnlinePlayers(int onlinePlayers) {
		this.onlinePlayers = Math.max(0, onlinePlayers);
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = Math.max(0, maxPlayers);
	}

	public List<ServerStatusSample> getSample() {
		return Collections.unmodifiableList(sample);
	}

	public void setSample(Collection<ServerStatusSample> profiles) {
		sample.clear();
		if (profiles != null) {
			for (ServerStatusSample profile : profiles) if (profile != null) sample.add(profile);
		}
	}

	public void addSample(ServerStatusSample profile) {
		if (profile != null) sample.add(profile);
	}
}
