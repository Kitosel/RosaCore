package pl.kiosel.rosacore.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.ZMaterial;

import java.util.*;

public class PlayerUtils {

	public static int getTotalExperience(Player player) {
		Objects.requireNonNull(player, "player");
		int level = player.getLevel();
		float expProgress = player.getExp();

		int totalExp = getExpAtLevel(level);
		totalExp += Math.round(expProgress * getExpToNextLevel(level));
		return totalExp;
	}

	public static boolean removeExperience(Player player, int amount) {
		Objects.requireNonNull(player, "player");
		if (amount < 0) throw new IllegalArgumentException("Experience amount cannot be negative");
		int current = getTotalExperience(player);
		if (current < amount) return false;

		int newTotal = current - amount;
		setTotalExperience(player, newTotal);
		return true;
	}

	public static void setTotalExperience(Player player, int amount) {
		Objects.requireNonNull(player, "player");
		if (amount < 0) throw new IllegalArgumentException("Experience amount cannot be negative");
		player.setExp(0);
		player.setLevel(0);
		player.setTotalExperience(0);

		int remaining = amount;
		while (remaining > 0) {
			int expToNext = getExpToNextLevel(player.getLevel());
			if (remaining >= expToNext) {
				remaining -= expToNext;
				player.setLevel(player.getLevel() + 1);
			} else {
				player.setExp((float) remaining / expToNext);
				remaining = 0;
			}
		}
	}

	public static int getExpAtLevel(int level) {
		if (level < 0) throw new IllegalArgumentException("Level cannot be negative");
		if (level <= 16) return (int) (Math.pow(level, 2) + 6 * level);
		if (level <= 31) return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
		return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
	}

	public static int getExpToNextLevel(int level) {
		if (level < 0) throw new IllegalArgumentException("Level cannot be negative");
		if (level <= 15) return 2 * level + 7;
		if (level <= 30) return 5 * level - 38;
		return 9 * level - 158;
	}

	public static Map<ZMaterial, Integer> getMissingItems(Player player, List<ItemStack> required) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(required, "required");
		Map<ZMaterial, Integer> missing = new HashMap<>();

		for (RequiredItem req : aggregate(required)) {
			int found = 0;

			for (ItemStack invItem : player.getInventory().getContents()) {
				if (invItem != null && invItem.isSimilar(req.item)) {
					found += invItem.getAmount();
				}
			}

			if (found < req.amount) {
				missing.merge(ZMaterial.from(req.item), req.amount - found, Integer::sum);
			}
		}

		return missing;
	}

	public static boolean hasEnoughItems(Player player, List<ItemStack> requiredItems) {
		return getMissingItems(player, requiredItems).isEmpty();
	}

	public static boolean hasEnoughItems(Player player, ItemStack requiredItems) {
		Objects.requireNonNull(requiredItems, "requiredItems");
		return hasEnoughItems(player, Collections.singletonList(requiredItems));
	}

	public static int countItems(Player player, ZMaterial material) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(material, "material");
		int amount = 0;
		for (ItemStack item : player.getInventory().getContents()) {
			if (item != null && material.matches(item)) {
				amount = Math.addExact(amount, item.getAmount());
			}
		}
		return amount;
	}

	public static int countItems(Player player, ItemStack template) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(template, "template");
		int amount = 0;
		for (ItemStack item : player.getInventory().getContents()) {
			if (item != null && item.isSimilar(template)) {
				amount = Math.addExact(amount, item.getAmount());
			}
		}
		return amount;
	}

	public static boolean hasItems(Player player, ZMaterial material, int amount) {
		requireAmount(amount);
		return countItems(player, material) >= amount;
	}

	public static boolean hasItems(Player player, ItemStack template, int amount) {
		requireAmount(amount);
		return countItems(player, template) >= amount;
	}

	public static boolean takeItems(Player player, List<ItemStack> requiredItems) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(requiredItems, "requiredItems");
		List<RequiredItem> required = aggregate(requiredItems);
		if (!hasEnoughItems(player, requiredItems)) {
			return false;
		}
		for (RequiredItem requirement : required) {
			removeMatching(player, requirement.item, requirement.amount);
		}
		return true;
	}

	public static boolean takeItems(Player player, ItemStack requiredItem) {
		Objects.requireNonNull(requiredItem, "requiredItem");
		return takeItems(player, Collections.singletonList(requiredItem));
	}

	public static boolean removeItems(Player player, ZMaterial material, int amount) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(material, "material");
		requireAmount(amount);
		if (countItems(player, material) < amount) {
			return false;
		}

		int remaining = amount;
		ItemStack[] contents = player.getInventory().getContents();
		for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
			ItemStack current = contents[slot];
			if (current == null || !material.matches(current)) continue;
			remaining -= removeFromSlot(player, slot, current, remaining);
		}
		return true;
	}

	public static Map<Integer, ItemStack> addItems(Player player, ItemStack... items) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(items, "items");
		ItemStack[] copies = new ItemStack[items.length];
		for (int index = 0; index < items.length; index++) {
			copies[index] = Objects.requireNonNull(items[index], "item").clone();
		}
		Map<Integer, ItemStack> leftovers = player.getInventory().addItem(copies);
		Map<Integer, ItemStack> result = new LinkedHashMap<>();
		for (Map.Entry<Integer, ItemStack> entry : leftovers.entrySet()) {
			result.put(entry.getKey(), entry.getValue().clone());
		}
		return Collections.unmodifiableMap(result);
	}

	public static Map<Integer, ItemStack> addItems(Player player, Collection<ItemStack> items) {
		Objects.requireNonNull(items, "items");
		return addItems(player, items.toArray(new ItemStack[0]));
	}

	public static boolean canFitItems(Player player, ItemStack... items) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(items, "items");
		ItemStack[] simulated = cloneContents(player.getInventory().getContents());

		for (ItemStack input : items) {
			ItemStack item = Objects.requireNonNull(input, "item");
			int remaining = item.getAmount();
			if (remaining <= 0) continue;

			for (ItemStack current : simulated) {
				if (current == null || !current.isSimilar(item)) continue;
				int maxStack = Math.min(current.getMaxStackSize(), item.getMaxStackSize());
				int moved = Math.min(Math.max(0, maxStack - current.getAmount()), remaining);
				current.setAmount(current.getAmount() + moved);
				remaining -= moved;
				if (remaining == 0) break;
			}

			for (int slot = 0; slot < simulated.length && remaining > 0; slot++) {
				if (!isEmpty(simulated[slot])) continue;
				int moved = Math.min(item.getMaxStackSize(), remaining);
				ItemStack placed = item.clone();
				placed.setAmount(moved);
				simulated[slot] = placed;
				remaining -= moved;
			}

			if (remaining > 0) return false;
		}
		return true;
	}

	public static boolean canFitItems(Player player, Collection<ItemStack> items) {
		Objects.requireNonNull(items, "items");
		return canFitItems(player, items.toArray(new ItemStack[0]));
	}

	public static int getFreeSlots(Player player) {
		Objects.requireNonNull(player, "player");
		int free = 0;
		for (ItemStack item : player.getInventory().getContents()) {
			if (isEmpty(item)) free++;
		}
		return free;
	}

	public static void giveItem(Player player, ItemStack item) {
		if (player == null || !player.isOnline() || item == null) {
			return;
		}

		Map<Integer, ItemStack> leftover = addItems(player, item);

		if (!leftover.isEmpty()) {
			leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
		}
	}

	public static void giveItem(Player player, ItemStack... items) {
		if (player == null || !player.isOnline() || items == null || items.length == 0) {
			return;
		}

		Map<Integer, ItemStack> leftover = addItems(player, items);
		if (!leftover.isEmpty()) {
			final World world = player.getWorld();
			final Location location = player.getLocation();

			leftover.values().forEach(it -> world.dropItemNaturally(location, it));
		}
	}

	public static void giveItem(Player player, Collection<ItemStack> items) {
		if (player == null || !player.isOnline() || items == null || items.isEmpty()) {
			return;
		}

		Map<Integer, ItemStack> leftover = addItems(player, items);

		if (!leftover.isEmpty()) {
			final World world = player.getWorld();
			final Location location = player.getLocation();
			leftover.values().forEach(it -> world.dropItemNaturally(location, it));
		}
	}

	public static void removeItem(Player player, ItemStack item) {
		if (player == null || !player.isOnline() || item == null) {
			return;
		}
		player.getInventory().removeItem(item);
	}

	public static void removeItem(Player player, ItemStack... items) {
		if (player == null || !player.isOnline() || items == null || items.length == 0) {
			return;
		}
		player.getInventory().removeItem(items);
	}

	public static void removeItem(Player player, Collection<ItemStack> items) {
		if (player == null || !player.isOnline() || items == null || items.isEmpty()) {
			return;
		}
		player.getInventory().removeItem(items.toArray(new ItemStack[0]));
	}

	private static List<RequiredItem> aggregate(List<ItemStack> required) {
		List<RequiredItem> aggregated = new ArrayList<>();
		for (ItemStack item : required) {
			if (item == null || item.getAmount() <= 0) continue;
			RequiredItem match = null;
			for (RequiredItem candidate : aggregated) {
				if (candidate.item.isSimilar(item)) {
					match = candidate;
					break;
				}
			}
			if (match == null) {
				aggregated.add(new RequiredItem(item.clone(), item.getAmount()));
			} else {
				match.amount = Math.addExact(match.amount, item.getAmount());
			}
		}
		return aggregated;
	}

	private static void removeMatching(Player player, ItemStack template, int amount) {
		int remaining = amount;
		ItemStack[] contents = player.getInventory().getContents();
		for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
			ItemStack current = contents[slot];
			if (current == null || !current.isSimilar(template)) continue;
			remaining -= removeFromSlot(player, slot, current, remaining);
		}
	}

	private static int removeFromSlot(Player player, int slot, ItemStack current, int requested) {
		int removed = Math.min(current.getAmount(), requested);
		if (removed == current.getAmount()) {
			player.getInventory().setItem(slot, null);
		} else {
			ItemStack changed = current.clone();
			changed.setAmount(current.getAmount() - removed);
			player.getInventory().setItem(slot, changed);
		}
		return removed;
	}

	private static ItemStack[] cloneContents(ItemStack[] contents) {
		ItemStack[] copy = new ItemStack[contents.length];
		for (int index = 0; index < contents.length; index++) {
			copy[index] = contents[index] == null ? null : contents[index].clone();
		}
		return copy;
	}

	private static boolean isEmpty(ItemStack item) {
		return item == null || item.getType().name().equals("AIR") || item.getAmount() <= 0;
	}

	private static void requireAmount(int amount) {
		if (amount < 0) throw new IllegalArgumentException("Item amount cannot be negative");
	}

	private static final class RequiredItem {
		private final ItemStack item;
		private int amount;

		private RequiredItem(ItemStack item, int amount) {
			this.item = item;
			this.amount = amount;
		}
	}
}
