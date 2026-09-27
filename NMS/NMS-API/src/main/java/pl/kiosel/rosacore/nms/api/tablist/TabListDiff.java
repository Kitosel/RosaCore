package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.GameMode;

import java.util.Objects;

public final class TabListDiff {

	private final boolean recreate;
	private final boolean textChanged;
	private final boolean pingChanged;
	private final boolean gameModeChanged;

	private TabListDiff(boolean recreate, boolean textChanged, boolean pingChanged,
						boolean gameModeChanged) {
		this.recreate = recreate;
		this.textChanged = textChanged;
		this.pingChanged = pingChanged;
		this.gameModeChanged = gameModeChanged;
	}

	public static TabListDiff between(TabListCell[] previous, TabListCell[] current) {
		if (previous == null || current == null || previous.length != current.length) {
			return new TabListDiff(true, true, true, true);
		}
		boolean recreate = false;
		boolean text = false;
		boolean ping = false;
		boolean gameMode = false;
		for (int slot = 0; slot < current.length; slot++) {
			TabListCell before = previous[slot];
			TabListCell after = current[slot];
			recreate |= !Objects.equals(uniqueId(before), uniqueId(after))
					|| !Objects.equals(profileName(before), profileName(after))
					|| !Objects.equals(skin(before), skin(after))
					|| listed(before) != listed(after);
			text |= !Objects.equals(text(before), text(after));
			ping |= ping(before) != ping(after);
			gameMode |= gameMode(before) != gameMode(after);
		}
		return new TabListDiff(recreate, text, ping, gameMode);
	}

	public boolean requiresRecreate() {
		return recreate;
	}

	public boolean isTextChanged() {
		return textChanged;
	}

	public boolean isPingChanged() {
		return pingChanged;
	}

	public boolean isGameModeChanged() {
		return gameModeChanged;
	}

	public boolean isEmpty() {
		return !recreate && !textChanged && !pingChanged && !gameModeChanged;
	}

	private static java.util.UUID uniqueId(TabListCell cell) {
		return cell == null ? null : cell.getUniqueId();
	}

	private static String profileName(TabListCell cell) {
		return cell == null ? null : cell.getProfileName();
	}

	private static String text(TabListCell cell) {
		return cell == null || cell.getText() == null ? "" : cell.getText();
	}

	private static int ping(TabListCell cell) {
		return cell == null ? 0 : Math.max(0, cell.getPing());
	}

	private static TabListSkin skin(TabListCell cell) {
		return cell == null ? null : cell.getSkin();
	}

	private static GameMode gameMode(TabListCell cell) {
		return cell == null || cell.getGameMode() == null ? GameMode.SURVIVAL : cell.getGameMode();
	}

	private static boolean listed(TabListCell cell) {
		return cell == null || cell.isListed();
	}
}
