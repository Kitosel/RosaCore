package pl.kiosel.rosacore.material;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.ZMaterial;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public final class SkullItemCreator {

	private SkullItemCreator() {
	}

	public static ItemStack byTextureValue(String textureValue) {
		Objects.requireNonNull(textureValue, "textureValue");
		ItemStack head = ZMaterial.PLAYER_HEAD.requireItem();
		NBT.modify(head, nbt -> {
			ReadWriteNBT owner = nbt.getOrCreateCompound("SkullOwner");
			owner.setUUID("Id", UUID.randomUUID());
			ReadWriteNBT properties = owner.getOrCreateCompound("Properties");
			ReadWriteNBT texture = properties.getCompoundList("textures").addCompound();
			texture.setString("Value", textureValue);
		});
		return head;
	}

	public static ItemStack byTextureUrl(String textureUrl) {
		Objects.requireNonNull(textureUrl, "textureUrl");
		String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + escapeJson(textureUrl) + "\"}}}";
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		return byTextureValue(encoded);
	}

	public static ItemStack byTextureUrlHash(String textureHash) {
		Objects.requireNonNull(textureHash, "textureHash");
		String hash = textureHash.trim();
		return byTextureUrl("https://textures.minecraft.net/texture/" + hash);
	}

	private static String escapeJson(String text) {
		return text.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
