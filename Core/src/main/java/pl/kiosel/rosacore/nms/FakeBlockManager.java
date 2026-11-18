package pl.kiosel.rosacore.nms;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;

import java.util.Map;

public class FakeBlockManager {

	private final RosaPlugin plugin;

	public FakeBlockManager(RosaPlugin plugin) {
		this.plugin = plugin;
	}

	public void createForPlayer(Player player, Location location, Material material) {
		plugin.getNMS().getFakeBlockFactory().createForPlayer(player, location, material);
	}

	public void createForPlayer(Player player, Map<Location, Material> schematic) {
		plugin.getNMS().getFakeBlockFactory().createForPlayer(player, schematic);
	}

	public void createForPlayer(Iterable<? extends Player> players, Location location, Material material) {
		players.forEach(player -> createForPlayer(player, location, material));
	}

	public void createForPlayer(Iterable<? extends Player> players, Map<Location, Material> schematic) {
		players.forEach(player -> createForPlayer(player, schematic));
	}
}
