package pl.kiosel.rosacore.hook.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.hook.internal.HookReflection;

public final class VaultEconomyHook extends EconomyHook {

	private Object provider;
	private Class<?> economyType;
	private Economy econ = null;

	@Override
	public String getName() {
		return "Vault";
	}

	@Override
	public String[] getPluginDependencies() {
		return new String[]{"Vault"};
	}

	@Override
	protected boolean onEnable(RosaPlugin plugin) throws Exception {
		Plugin vault = getDependencyPlugin("Vault");
		this.economyType = HookReflection.findClass(vault, "net.milkbowl.vault.economy.Economy");
		return refreshProvider() && setupEconomy(plugin);
	}

	private boolean setupEconomy(RosaPlugin plugin) {
		if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	@Override
	protected void onDisable() {
		this.provider = null;
		this.economyType = null;
		this.econ = null;
	}

	@Override
	public double getBalance(OfflinePlayer player) {
		validatePlayer(player);
		return econ.getBalance(player);
		/*try {
			Object value = HookReflection.invoke(requireProvider(), "getBalance", player);
			return ((Number) value).doubleValue();
		} catch (ReflectiveOperationException exception) {
			throw economyFailure("read a balance", exception);
		}*/
	}

	@Override
	public boolean hasBalance(OfflinePlayer player, double amount) {
		validatePlayer(player);
		validateAmount(amount);
		return econ.has(player, amount);
		/*try {
			return Boolean.TRUE.equals(HookReflection.invoke(requireProvider(), "has", player, amount));
		} catch (ReflectiveOperationException exception) {
			throw economyFailure("check a balance", exception);
		}*/
	}

	@Override
	public boolean withdraw(OfflinePlayer player, double amount) {
		validatePlayer(player);
		validateAmount(amount);
		return econ.withdrawPlayer(player, amount).transactionSuccess();
		//return transaction("withdrawPlayer", validatePlayer(player), validateAmount(amount));
	}

	@Override
	public boolean deposit(OfflinePlayer player, double amount) {
		validatePlayer(player);
		validateAmount(amount);
		return econ.depositPlayer(player, amount).transactionSuccess();
		//return transaction("depositPlayer", validatePlayer(player), validateAmount(amount));
	}

	private boolean transaction(String method, OfflinePlayer player, double amount) {
		try {
			Object response = HookReflection.invoke(requireProvider(), method, player, amount);
			return response != null && Boolean.TRUE.equals(HookReflection.invoke(response, "transactionSuccess"));
		} catch (ReflectiveOperationException exception) {
			throw economyFailure(method.equals("depositPlayer") ? "deposit money" : "withdraw money", exception);
		}
	}

	private Object requireProvider() throws ReflectiveOperationException {
		if (!refreshProvider()) throw new IllegalStateException("Vault has no registered economy provider");
		return this.provider;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private boolean refreshProvider() throws ReflectiveOperationException {
		if (this.economyType == null) return false;
		RegisteredServiceProvider<?> registration = getPlugin().getServer().getServicesManager()
				.getRegistration((Class) this.economyType);
		if (registration == null || !registration.getPlugin().isEnabled()) {
			this.provider = null;
			return false;
		}
		this.provider = registration.getProvider();
		return this.provider != null;
	}
}
