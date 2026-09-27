package pl.kiosel.rosacore.nms.v1_13_R2.tablist;

import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.tablist.TabList;
import pl.kiosel.rosacore.nms.api.tablist.TabListService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NMSTabListService implements TabListService {

	private final Map<UUID, NMSTabList> tabLists = new HashMap<>();

	@Override
	public TabList create(Player player) {
		if (player == null) throw new NullPointerException("player");
		clear(player);
		NMSTabList tabList = new NMSTabList(this, player);
		tabLists.put(player.getUniqueId(), tabList);
		return tabList;
	}

	@Override
	public TabList get(Player player) {
		if (player == null) return null;
		return tabLists.get(player.getUniqueId());
	}

	@Override
	public void clear(Player player) {
		if (player == null) throw new NullPointerException("player");
		NMSTabList tabList = tabLists.remove(player.getUniqueId());
		if (tabList != null) tabList.clearInternal();
	}

	@Override
	public void clearAll() {
		NMSTabList[] existing = tabLists.values().toArray(new NMSTabList[0]);
		tabLists.clear();
		for (NMSTabList tabList : existing) tabList.clearInternal();
	}

	@Override
	public int size() {
		return tabLists.size();
	}

	void remove(NMSTabList tabList) {
		tabLists.remove(tabList.getPlayer().getUniqueId(), tabList);
	}
}
