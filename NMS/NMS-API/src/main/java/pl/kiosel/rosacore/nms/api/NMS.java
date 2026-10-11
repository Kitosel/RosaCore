package pl.kiosel.rosacore.nms.api;

import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.packet.TabPacketInterceptor;
import pl.kiosel.rosacore.nms.api.packet.TabPacketInterceptors;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.api.status.ServerStatusInterceptor;
import pl.kiosel.rosacore.nms.api.status.ServerStatusInterceptors;
import pl.kiosel.rosacore.nms.api.tablist.NMSTabListService;
import pl.kiosel.rosacore.nms.api.tablist.TabListService;

public interface NMS {

	CustomAnvilFactory getCustomAnvilFactory();

	NmsServer getNmsServer();

	default TabListService getTabListService() {
		return new NMSTabListService();
	}

	default TabPacketInterceptor getTabPacketInterceptor() {
		return TabPacketInterceptors.shared();
	}

	default ServerStatusInterceptor getServerStatusInterceptor() {
		return ServerStatusInterceptors.shared();
	}
}
