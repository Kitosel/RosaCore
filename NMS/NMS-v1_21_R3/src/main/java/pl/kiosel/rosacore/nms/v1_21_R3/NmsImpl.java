package pl.kiosel.rosacore.nms.v1_21_R3;

import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.entity.FakeEntityFactory;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.v1_21_R3.anvil.NMSCustomAnvilFactory;
import pl.kiosel.rosacore.nms.v1_21_R3.entity.FakeEntityFactoryImpl;
import pl.kiosel.rosacore.nms.v1_21_R3.server.ServerImpl;

public final class NmsImpl implements NMS {

	private final CustomAnvilFactory customAnvilFactory;
	private final NmsServer nmsServer;
	private final FakeEntityFactory fakeEntityFactory;

	public NmsImpl() {
		this.customAnvilFactory = new NMSCustomAnvilFactory();
		this.nmsServer = new ServerImpl();
		this.fakeEntityFactory = new FakeEntityFactoryImpl();
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
