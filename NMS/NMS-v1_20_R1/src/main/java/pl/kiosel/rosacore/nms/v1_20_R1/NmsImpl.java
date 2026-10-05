package pl.kiosel.rosacore.nms.v1_20_R1;

import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.api.toasts.NmsToasts;
import pl.kiosel.rosacore.nms.v1_20_R1.anvil.NMSCustomAnvilFactory;
import pl.kiosel.rosacore.nms.v1_20_R1.server.ServerImpl;
import pl.kiosel.rosacore.nms.v1_20_R1.toast.ToastImpl;

public final class NmsImpl implements NMS {

	private final CustomAnvilFactory customAnvilFactory;
	private final NmsServer nmsServer;
	private final NmsToasts toasts;

	public NmsImpl() {
		this.customAnvilFactory = new NMSCustomAnvilFactory();
		this.nmsServer = new ServerImpl();
		this.toasts = new ToastImpl();
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
	public NmsToasts getToasts() {
		return this.toasts;
	}
}
