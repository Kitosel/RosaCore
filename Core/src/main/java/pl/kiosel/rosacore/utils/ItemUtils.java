package pl.kiosel.rosacore.utils;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.material.ItemCreator;

import java.util.Collections;
import java.util.List;

public class ItemUtils {

	public static ItemStack create(ZMaterial material, int amount, String name, List<String> lore, boolean glow) {
		ItemCreator itemCreator = ItemCreator.of(material).amount(amount).name(name).glow(glow)
				.hideAttributes().hideAll();
		if (lore != null) {
			itemCreator.lore(lore);
		}
		return itemCreator.make();
	}

	public static ItemStack create(ZMaterial mat, int amount, String name) {
		return create(mat, amount, name, null, false);
	}

	public static ItemStack create(ZMaterial mat, String name, List<String> lore, boolean glow) {
		return create(mat, 1, name, lore, glow);
	}

	public static ItemStack create(ZMaterial mat, String name, String lore) {
		return create(mat, name, Collections.singletonList(lore));
	}

	public static ItemStack create(ZMaterial mat, String name, List<String> lore) {
		return create(mat, 1, name, lore, false);
	}

	public static ItemStack create(ZMaterial mat, int amount, String name, List<String> lore) {
		return create(mat, amount, name, lore, false);
	}

	public static ItemStack create(ZMaterial mat, String name) {
		return create(mat, 1, name, null, false);
	}

	public static ItemStack addGlow(ItemStack item) {
		return ItemCreator.of(item).glow(true).make();
	}

	public static ItemStack removeGlow(ItemStack item) {
		return ItemCreator.of(item).glow(false).clearEnchantments().make();
	}

	public static ItemStack addLore(ItemStack original, List<String> loreToAdd) {
		if (original == null || loreToAdd == null || loreToAdd.isEmpty()) return original;
		return ItemCreator.of(original).lore(loreToAdd).make();
	}

	public static ItemStack blank(Blank blank) {
		return create(blank.getMaterial(), blank.getFallbackName());
	}

	public enum Blank {
		WHITE(ZMaterial.WHITE_STAINED_GLASS_PANE, "&7&kBlank"),
		GRAY(ZMaterial.GRAY_STAINED_GLASS, "&7&kBlank"),
		BLACK(ZMaterial.BLACK_STAINED_GLASS, "&7&kBlank"),
		BACK(ZMaterial.ARROW, "&9Back"),
		NEXT_PAGE(ZMaterial.SPECTRAL_ARROW, "&9Next"),
		PREVIOUS_PAGE(ZMaterial.SPECTRAL_ARROW, "&cPrevious"),
		RESULT(ZMaterial.SPECTRAL_ARROW, "&e→"),
		EXIT(ZMaterial.ARROW, "&cExit");

		@Getter private final ZMaterial material;
		@Getter private final String fallbackName;

		Blank(ZMaterial material, String fallbackName) {
			this.material = material;
			this.fallbackName = fallbackName;
		}
	}
}
