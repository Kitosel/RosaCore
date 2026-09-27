package pl.kiosel.rosacore.hook.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.hook.internal.HookReflection;

public final class PlayerPointsEconomyHook extends EconomyHook {

	private Object api;

	@Override
	public String getName() {
		return "PlayerPoints";
	}

	@Override
	public String[] getPluginDependencies() {
		return new String[]{"PlayerPoints"};
	}

	@Override
	protected boolean onEnable(RosaPlugin plugin) throws Exception {
		Plugin playerPoints = getDependencyPlugin("PlayerPoints");
		this.api = HookReflection.invoke(playerPoints, "getAPI");
		return this.api != null;
	}

	@Override
	protected void onDisable() {
		this.api = null;
	}

	@Override
	public double getBalance(OfflinePlayer player) {
		validatePlayer(player);
		try {
			return ((Number) HookReflection.invoke(requireApi(), "look", player.getUniqueId())).doubleValue();
		} catch (ReflectiveOperationException exception) {
			throw economyFailure("read points", exception);
		}
	}

	@Override
	public boolean withdraw(OfflinePlayer player, double amount) {
		return change("take", validatePlayer(player), toPoints(validateAmount(amount)));
	}

	@Override
	public boolean deposit(OfflinePlayer player, double amount) {
		return change("give", validatePlayer(player), toPoints(validateAmount(amount)));
	}

	private boolean change(String operation, OfflinePlayer player, int amount) {
		try {
			return Boolean.TRUE.equals(HookReflection.invoke(requireApi(), operation, player.getUniqueId(), amount));
		} catch (ReflectiveOperationException exception) {
			throw economyFailure(operation + " points", exception);
		}
	}

	private Object requireApi() {
		if (this.api == null) throw new IllegalStateException("PlayerPoints API is unavailable");
		return this.api;
	}

	private int toPoints(double amount) {
		double rounded = Math.ceil(amount);
		if (rounded > Integer.MAX_VALUE)
			throw new IllegalArgumentException("PlayerPoints amount is too large");
		return (int) rounded;
	}
}
