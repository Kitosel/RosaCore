package pl.kiosel.rosacore.hook.economy;

import org.bukkit.OfflinePlayer;
import pl.kiosel.rosacore.hook.RosaHook;

import java.util.Objects;

public abstract class EconomyHook extends RosaHook {

	public abstract double getBalance(OfflinePlayer player);

	public boolean hasBalance(OfflinePlayer player, double amount) {
		validatePlayer(player);
		validateAmount(amount);
		return getBalance(player) >= amount;
	}

	public abstract boolean withdraw(OfflinePlayer player, double amount);

	public final boolean withdrawBalance(OfflinePlayer player, double amount) {
		return withdraw(player, amount);
	}

	public abstract boolean deposit(OfflinePlayer player, double amount);

	protected final OfflinePlayer validatePlayer(OfflinePlayer player) {
		return Objects.requireNonNull(player, "player");
	}

	protected final double validateAmount(double amount) {
		if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0.0D)
			throw new IllegalArgumentException("Economy amount must be finite and at least zero");
		return amount;
	}

	protected final IllegalStateException economyFailure(String operation, Exception cause) {
		return new IllegalStateException(getName() + " failed to " + operation, cause);
	}
}
