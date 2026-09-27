package pl.kiosel.rosacore.nms.api.status;

@FunctionalInterface
public interface ServerStatusPacketListener {

	void onServerStatus(ServerStatusPacketEvent event);
}
