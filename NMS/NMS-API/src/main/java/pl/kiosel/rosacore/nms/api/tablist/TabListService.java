package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.entity.Player;

public interface TabListService extends AutoCloseable {

	TabList create(Player player);

	TabList get(Player player);

	default TabList getOrCreate(Player player) {
		TabList existing = get(player);
		return existing == null ? create(player) : existing;
	}

	default boolean has(Player player) {
		return get(player) != null;
	}

	void clear(Player player);

	void clearAll();

	int size();

	@Override
	default void close() {
		clearAll();
	}
}
