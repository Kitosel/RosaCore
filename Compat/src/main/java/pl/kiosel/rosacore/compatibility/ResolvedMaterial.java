package pl.kiosel.rosacore.compatibility;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.OptionalInt;

public final class ResolvedMaterial {

	private final String canonicalName;
	private final Material material;
	private final int legacyData;

	ResolvedMaterial(String canonicalName, Material material, int legacyData) {
		this.canonicalName = canonicalName;
		this.material = material;
		this.legacyData = legacyData;
	}

	public String getCanonicalName() {
		return this.canonicalName;
	}

	public Material getMaterial() {
		return this.material;
	}

	public boolean hasLegacyData() {
		return this.legacyData >= 0;
	}

	public OptionalInt getLegacyData() {
		return this.hasLegacyData() ? OptionalInt.of(this.legacyData) : OptionalInt.empty();
	}

	public ItemStack createItem(int amount) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Item amount must be positive");
		}
		return this.hasLegacyData()
				? new ItemStack(this.material, amount, (short) this.legacyData)
				: new ItemStack(this.material, amount);
	}

	public boolean matches(Material material) {
		return this.material == Objects.requireNonNull(material, "material");
	}

	public boolean matches(ItemStack item) {
		Objects.requireNonNull(item, "item");
		return item.getType() == this.material
				&& (!this.hasLegacyData() || (item.getDurability() & 0xFFFF) == this.legacyData);
	}

	public boolean matches(Block block) {
		Objects.requireNonNull(block, "block");
		return block.getType() == this.material
				&& (!this.hasLegacyData() || (block.getData() & 0xFF) == this.legacyData);
	}

	@Override
	public String toString() {
		return this.hasLegacyData()
				? this.material.name() + ":" + this.legacyData
				: this.material.name();
	}
}
