package pl.kiosel.rosacore.nms.v26_3_R1.tablist;

import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.tablist.ReflectiveTabListService;
import pl.kiosel.rosacore.nms.api.tablist.TabList;
import pl.kiosel.rosacore.nms.api.tablist.TabListService;

public final class NMSTabListService implements TabListService {
	private final ReflectiveTabListService delegate = new ReflectiveTabListService();

	@Override
	public TabList create(Player player) {
		return delegate.create(player);
	}

	@Override
	public TabList get(Player player) {
		return delegate.get(player);
	}

	@Override
	public void clear(Player player) {
		delegate.clear(player);
	}

	@Override
	public void clearAll() {
		delegate.clearAll();
	}

	@Override
	public int size() {
		return delegate.size();
	}
}
