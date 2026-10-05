package pl.kiosel.rosacore.nms.api.toasts;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface NmsToasts {

	void sendToast(Plugin plugin, Player player, String titleText, Material material, NmsAdvancementType frameType);

	enum NmsAdvancementType {
		TASK("TASK"),
		CHALLENGE("CHALLENGE"),
		GOAL("GOAL");

		private final String name;

		NmsAdvancementType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}
