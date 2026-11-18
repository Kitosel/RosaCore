package pl.kiosel.rosacore.nms.v1_21_R7;

import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.entity.FakeEntityFactory;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.v1_21_R7.anvil.NMSCustomAnvilFactory;
import pl.kiosel.rosacore.nms.v1_21_R7.entity.FakeEntityFactoryImpl;
import pl.kiosel.rosacore.nms.v1_21_R7.server.ServerImpl;

public final class NmsImpl implements NMS {

	private final CustomAnvilFactory customAnvilFactory;
	private final FakeEntityFactory fakeEntityFactory;
	private final NmsServer nmsServer;

	public NmsImpl() {
		this.customAnvilFactory = new NMSCustomAnvilFactory();
		this.fakeEntityFactory = new FakeEntityFactoryImpl();
		this.nmsServer = new ServerImpl();
	}

	@Override
	public CustomAnvilFactory getCustomAnvilFactory() {
		return this.customAnvilFactory;
	}

	@Override
	public NmsServer getNmsServer() {
		return this.nmsServer;
	}

	@Override
	public FakeEntityFactory getFakeItemFactory() {
		return this.fakeEntityFactory;
	}
}
