package pl.kiosel.rosacore.hook.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.hook.internal.HookReflection;

import java.math.BigDecimal;
import java.util.UUID;

public final class EssentialsXEconomyHook extends EconomyHook {

	private Class<?> economyType;

	@Override
	public String getName() {
		return "Essentials";
	}

	@Override
	public String[] getPluginDependencies() {
		return new String[]{"Essentials"};
	}

	@Override
	protected boolean onEnable(RosaPlugin plugin) throws Exception {
		Plugin essentials = getDependencyPlugin("Essentials");
		this.economyType = HookReflection.findClass(essentials, "com.earth2me.essentials.api.Economy");
		return true;
	}

	@Override
	protected void onDisable() {
		this.economyType = null;
	}

	@Override
	public double getBalance(OfflinePlayer player) {
		UUID playerId = validatePlayer(player).getUniqueId();
		try {
			Object value = HookReflection.invokeStaticExact(
					requireEconomyType(), "getMoneyExact", new Class<?>[]{UUID.class}, playerId);
			return ((Number) value).doubleValue();
		} catch (ReflectiveOperationException exception) {
			throw economyFailure("read a balance", exception);
		}
	}

	@Override
	public boolean hasBalance(OfflinePlayer player, double amount) {
		UUID playerId = validatePlayer(player).getUniqueId();
		BigDecimal exactAmount = BigDecimal.valueOf(validateAmount(amount));
		try {
			return Boolean.TRUE.equals(HookReflection.invokeStaticExact(
					requireEconomyType(), "hasEnough",
					new Class<?>[]{UUID.class, BigDecimal.class}, playerId, exactAmount));
		} catch (ReflectiveOperationException exception) {
			throw economyFailure("check a balance", exception);
		}
	}

	@Override
	public boolean withdraw(OfflinePlayer player, double amount) {
		return transaction("subtract", validatePlayer(player), validateAmount(amount));
	}

	@Override
	public boolean deposit(OfflinePlayer player, double amount) {
		return transaction("add", validatePlayer(player), validateAmount(amount));
	}

	private boolean transaction(String method, OfflinePlayer player, double amount) {
		UUID playerId = player.getUniqueId();
		BigDecimal exactAmount = BigDecimal.valueOf(amount);
		try {
			HookReflection.invokeStaticExact(
					requireEconomyType(), method,
					new Class<?>[]{UUID.class, BigDecimal.class}, playerId, exactAmount);
			return true;
		} catch (ReflectiveOperationException exception) {
			throw economyFailure(method.equals("add") ? "deposit money" : "withdraw money", exception);
		}
	}

	private Class<?> requireEconomyType() {
		if (this.economyType == null) throw new IllegalStateException("EssentialsX economy API is unavailable");
		return this.economyType;
	}
}
