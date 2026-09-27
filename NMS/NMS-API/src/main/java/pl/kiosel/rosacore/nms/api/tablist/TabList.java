package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface TabList {

	int DEFAULT_CELL_COUNT = 80;
	int ROW_COUNT = 20;
	int COLUMN_COUNT = 4;

	Player getPlayer();

	int getCellCount();

	int getActiveCellCount();

	TabList setActiveCellCount(int cellCount);

	String getHeader();

	String getFooter();

	TabList setHeader(String header);

	TabList setFooter(String footer);

	default TabList setHeaderFooter(String header, String footer) {
		return setHeader(header).setFooter(footer);
	}

	default TabList clearHeaderFooter() {
		return setHeaderFooter("", "");
	}

	TabListCell getCell(int slot);

	TabList setCell(int slot, TabListCell cell);

	default TabList setCell(int slot, String text) {
		return setCell(slot, TabListCell.of(text));
	}

	default TabList setCell(int slot, String text, int ping) {
		return setCell(slot, TabListCell.of(text, ping));
	}

	default TabList setCell(int slot, String text, int ping, TabListSkin skin) {
		return setCell(slot, TabListCell.of(text, ping, skin));
	}

	default TabListCell getCell(int row, int column) {
		return getCell(slot(row, column));
	}

	default TabList setCell(int row, int column, TabListCell cell) {
		return setCell(slot(row, column), cell);
	}

	default TabList setCell(int row, int column, String text) {
		return setCell(slot(row, column), text);
	}

	default TabList setCell(int row, int column, String text, int ping) {
		return setCell(slot(row, column), text, ping);
	}

	default TabList setCell(int row, int column, String text, int ping, TabListSkin skin) {
		return setCell(slot(row, column), text, ping, skin);
	}

	TabList clearCell(int slot);

	default TabList clearCell(int row, int column) {
		return clearCell(slot(row, column));
	}

	TabList clearCells();

	TabList showRealPlayer(Player realPlayer);

	TabList hideRealPlayer(Player realPlayer);

	default TabList hideRealPlayer(UUID profileId) {
		return hideRealPlayer(profileId == null ? null : Bukkit.getPlayer(profileId));
	}

	void sendHeaderFooter();

	void send();

	default void update() {
		send();
	}

	void clear();

	static int slot(int row, int column) {
		if (row < 0 || row >= ROW_COUNT) {
			throw new IndexOutOfBoundsException("Tab-list row must be between 0 and 19: " + row);
		}
		if (column < 0 || column >= COLUMN_COUNT) {
			throw new IndexOutOfBoundsException("Tab-list column must be between 0 and 3: " + column);
		}
		return column * ROW_COUNT + row;
	}
}
