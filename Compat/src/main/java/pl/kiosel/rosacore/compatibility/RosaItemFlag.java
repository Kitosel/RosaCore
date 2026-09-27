package pl.kiosel.rosacore.compatibility;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class RosaItemFlag {

	private static final NameAliases ALIASES = new NameAliases()
			.add("HIDE_ADDITIONAL_TOOLTIP", "HIDE_POTION_EFFECTS");

	private final String name;

	private RosaItemFlag(String name) {
		this.name = name;
	}

	public static RosaItemFlag of(String name) {
		return new RosaItemFlag(ALIASES.canonical(name));
	}

	public static Optional<RosaItemFlag> parse(String name) {
		try {
			return Optional.of(of(name));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public static RosaItemFlag from(ItemFlag flag) {
		return of(Objects.requireNonNull(flag, "flag").name());
	}

	public static List<RosaItemFlag> supported() {
		List<RosaItemFlag> flags = new ArrayList<>();
		for (ItemFlag flag : ItemFlag.values()) {
			flags.add(from(flag));
		}
		return Collections.unmodifiableList(flags);
	}

	public String getName() {
		return this.name;
	}

	public Optional<ItemFlag> resolve() {
		ItemFlag direct = valueOf(this.name);
		if (direct != null) {
			return Optional.of(direct);
		}
		String legacy = ALIASES.legacy(this.name);
		return Optional.ofNullable(legacy == null ? null : valueOf(legacy));
	}

	public boolean isSupported() {
		return this.resolve().isPresent();
	}

	public boolean apply(ItemMeta meta) {
		Objects.requireNonNull(meta, "meta");
		Optional<ItemFlag> flag = this.resolve();
		if (!flag.isPresent()) {
			return false;
		}
		meta.addItemFlags(flag.get());
		return true;
	}

	public boolean apply(ItemStack item) {
		Objects.requireNonNull(item, "item");
		ItemMeta meta = item.getItemMeta();
		if (meta == null || !this.apply(meta)) {
			return false;
		}
		item.setItemMeta(meta);
		return true;
	}

	public boolean remove(ItemMeta meta) {
		Objects.requireNonNull(meta, "meta");
		Optional<ItemFlag> flag = this.resolve();
		if (!flag.isPresent()) {
			return false;
		}
		meta.removeItemFlags(flag.get());
		return true;
	}

	private static ItemFlag valueOf(String name) {
		return NameAliases.staticValue(ItemFlag.class, name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
