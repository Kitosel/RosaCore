package pl.kiosel.rosacore.hook.economy;

import org.bukkit.OfflinePlayer;

import java.util.*;

public final class EconomyManager {

	private final Map<EconomyHook, Integer> hooks = new LinkedHashMap<>();
	private EconomyHook activeHook;
	private String preferredName;

	public void register(EconomyHook hook, int priority) {
		Objects.requireNonNull(hook, "hook");
		if (find(hook.getName()) != null)
			throw new IllegalArgumentException("Economy hook '" + hook.getName() + "' is already registered");
		this.hooks.put(hook, priority);
		refreshActiveHook();
	}

	public void unregister(EconomyHook hook) {
		if (hook == null) return;
		this.hooks.remove(hook);
		if (this.activeHook == hook) this.activeHook = null;
		refreshActiveHook();
	}

	public Optional<EconomyHook> getActiveHook() {
		refreshActiveHook();
		return Optional.ofNullable(this.activeHook);
	}

	public String getActiveName() {
		EconomyHook hook = getActiveHook().orElse(null);
		return hook == null ? null : hook.getName();
	}

	public boolean setPreferredHook(String name) {
		EconomyHook hook = find(name);
		if (hook == null || !hook.isEnabled()) return false;
		this.preferredName = hook.getName();
		this.activeHook = hook;
		return true;
	}

	public boolean findAndSetHook() {
		String find = null;
		for (String available : getAvailableNames()) {
			EconomyHook hook = find(available);
			if (hook == null || !hook.isEnabled()) return false;
			find = hook.getName();
		}
		return setPreferredHook(find);
	}

	public void clearPreferredHook() {
		this.preferredName = null;
		this.activeHook = null;
		refreshActiveHook();
	}

	public boolean isAvailable(String name) {
		EconomyHook hook = find(name);
		return hook != null && hook.isEnabled();
	}

	public List<String> getSupportedNames() {
		List<String> names = new ArrayList<>();
		for (EconomyHook hook : this.hooks.keySet()) names.add(hook.getName());
		return Collections.unmodifiableList(names);
	}

	public List<String> getAvailableNames() {
		List<String> names = new ArrayList<>();
		for (EconomyHook hook : this.hooks.keySet()) {
			if (hook.isEnabled()) names.add(hook.getName());
		}
		return Collections.unmodifiableList(names);
	}

	public double getBalance(OfflinePlayer player) {
		return requireActive().getBalance(player);
	}

	public boolean hasBalance(OfflinePlayer player, double amount) {
		return requireActive().hasBalance(player, amount);
	}

	public boolean withdraw(OfflinePlayer player, double amount) {
		return requireActive().withdraw(player, amount);
	}

	public boolean deposit(OfflinePlayer player, double amount) {
		return requireActive().deposit(player, amount);
	}

	public EconomyHook requireActive() {
		return getActiveHook().orElseThrow(() ->
				new IllegalStateException("No supported economy plugin is currently available"));
	}

	public void refreshActiveHook() {
		if (this.preferredName != null) {
			EconomyHook preferred = find(this.preferredName);
			if (preferred != null && preferred.isEnabled()) {
				this.activeHook = preferred;
				return;
			}
		}

		this.activeHook = this.hooks.entrySet().stream()
				.filter(entry -> entry.getKey().isEnabled())
				.max(Comparator.comparingInt(Map.Entry::getValue))
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	private EconomyHook find(String name) {
		if (name == null) return null;
		String expected = name.trim().toLowerCase(Locale.ROOT);
		for (EconomyHook hook : this.hooks.keySet()) {
			if (hook.getName().toLowerCase(Locale.ROOT).equals(expected)) return hook;
		}
		return null;
	}
}
