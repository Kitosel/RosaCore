package pl.kiosel.rosacore.nms.v1_19_R3;

import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.v1_19_R3.anvil.NMSCustomAnvilFactory;
import pl.kiosel.rosacore.nms.v1_19_R3.server.ServerImpl;

public final class NmsImpl implements NMS {

	private final CustomAnvilFactory customAnvilFactory;
	private final NmsServer nmsServer;

	public NmsImpl() {
		this.customAnvilFactory = new NMSCustomAnvilFactory();
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
}
