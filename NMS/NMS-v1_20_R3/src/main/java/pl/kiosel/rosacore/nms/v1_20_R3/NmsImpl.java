package pl.kiosel.rosacore.nms.v1_20_R4;

import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.tablist.TabListService;
import pl.kiosel.rosacore.nms.v1_20_R4.anvil.NMSCustomAnvilFactory;
import pl.kiosel.rosacore.nms.v1_20_R4.tablist.NMSTabListService;

public final class NmsImpl implements NMS {

	private final CustomAnvilFactory customAnvilFactory = new NMSCustomAnvilFactory();
	private final TabListService tabListService = new NMSTabListService();

	@Override
	public CustomAnvilFactory getCustomAnvilFactory() {
		return customAnvilFactory;
	}

	@Override
	public TabListService getTabListService() {
		return tabListService;
	}
}
