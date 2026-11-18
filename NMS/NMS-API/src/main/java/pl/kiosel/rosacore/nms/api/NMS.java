package pl.kiosel.rosacore.nms.api;

import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.entity.FakeEntityFactory;
import pl.kiosel.rosacore.nms.api.packet.TabPacketInterceptor;
import pl.kiosel.rosacore.nms.api.packet.TabPacketInterceptors;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.api.status.ServerStatusInterceptor;
import pl.kiosel.rosacore.nms.api.status.ServerStatusInterceptors;
import pl.kiosel.rosacore.nms.api.tablist.NMSTabListService;
import pl.kiosel.rosacore.nms.api.tablist.TabListService;
import pl.kiosel.rosacore.nms.api.world.FakeBlock;

public interface NMS {

	CustomAnvilFactory getCustomAnvilFactory();

	NmsServer getNmsServer();

	default FakeBlock getFakeBlockFactory() {
		throw new UnsupportedOperationException("Fake block are not supported by this NMS implementation");
	}

	default FakeEntityFactory getFakeItemFactory() {
		throw new UnsupportedOperationException("Fake items are not supported by this NMS implementation");
	}

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
