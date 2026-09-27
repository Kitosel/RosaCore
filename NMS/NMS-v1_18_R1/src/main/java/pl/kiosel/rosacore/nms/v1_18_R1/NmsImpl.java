package pl.kiosel.rosacore.nms.v1_18_R1;

import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.api.tablist.TabListService;
import pl.kiosel.rosacore.nms.v1_18_R1.anvil.NMSCustomAnvilFactory;
import pl.kiosel.rosacore.nms.v1_18_R1.server.ServerImpl;
import pl.kiosel.rosacore.nms.v1_18_R1.tablist.NMSTabListService;

public final class NmsImpl implements NMS {

	private final CustomAnvilFactory customAnvilFactory;
	private final TabListService tabListService;
	private final NmsServer nmsServer;

	public NmsImpl() {
		this.customAnvilFactory = new NMSCustomAnvilFactory();
		this.tabListService = new NMSTabListService();
		this.nmsServer = new ServerImpl();
	}

	@Override
	public CustomAnvilFactory getCustomAnvilFactory() {
		return this.customAnvilFactory;
	}

	@Override
	public TabListService getTabListService() {
		return this.tabListService;
	}

	@Override
	public NmsServer getNmsServer() {
		return this.nmsServer;
	}
}
