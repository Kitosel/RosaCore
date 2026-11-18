package pl.kiosel.rosacore.nms.v26;

import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.entity.FakeEntityFactory;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.api.world.FakeBlock;
import pl.kiosel.rosacore.nms.v26.anvil.NMSCustomAnvilFactory;
import pl.kiosel.rosacore.nms.v26.entity.FakeEntityFactoryImpl;
import pl.kiosel.rosacore.nms.v26.server.ServerImpl;
import pl.kiosel.rosacore.nms.v26.world.FakeBlockImpl;

public final class NmsImpl implements NMS {

	private final CustomAnvilFactory customAnvilFactory;
	private final FakeEntityFactory fakeEntityFactory;
	private final NmsServer nmsServer;
	private final FakeBlock fakeBlock;

	public NmsImpl() {
		this.customAnvilFactory = new NMSCustomAnvilFactory();
		this.fakeEntityFactory = new FakeEntityFactoryImpl();
		this.nmsServer = new ServerImpl();
		this.fakeBlock = new FakeBlockImpl();
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

	@Override
	public FakeBlock getFakeBlockFactory() {
		return this.fakeBlock;
	}
}
