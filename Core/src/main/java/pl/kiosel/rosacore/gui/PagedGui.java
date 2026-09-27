package pl.kiosel.rosacore.gui;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.RosaSound;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.compatibility.ZSound;
import pl.kiosel.rosacore.material.ItemCreator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class PagedGui extends Gui {

	private static final int PREVIOUS_ACTION_SLOT = -1001;
	private static final int NEXT_ACTION_SLOT = -1002;
	private static final int FOOTER_ACTION_BASE = -1100;
	private static final int PREVIOUS_COLUMN = 3;
	private static final int NEXT_COLUMN = 5;

	private final Map<Integer, ItemStack> footerItems = new HashMap<>();
	private boolean useHeader;
	private boolean useFooter = true;
	private boolean autoSize = true;
	private int page = 1;
	private int pages = 1;
	private ItemStack headerBackItem;
	private ItemStack footerBackItem;
	private ItemStack previousPageItem;
	private ItemStack nextPageItem;

	protected PagedGui() {
		super(2);
		configureNavigationActions();
	}

	protected PagedGui(Gui parent) {
		super(2, parent);
		configureNavigationActions();
	}

	protected PagedGui(int rows) {
		super(validatePagedRows(rows));
		this.autoSize = false;
		configureNavigationActions();
	}

	protected PagedGui(int rows, Gui parent) {
		super(validatePagedRows(rows), parent);
		this.autoSize = false;
		configureNavigationActions();
	}

	public final int getPage() {
		recalculateLayout(false);
		return page;
	}

	public final int getPages() {
		recalculateLayout(false);
		return pages;
	}

	public final boolean usesHeader() {
		return useHeader;
	}

	public final boolean usesFooter() {
		return useFooter;
	}

	public PagedGui setUseFooter(boolean useFooter) {
		if (this.useFooter != useFooter) {
			this.useFooter = useFooter;
			recalculateLayout(true);
		}
		return this;
	}

	public PagedGui setUseHeader(boolean useHeader) {
		if (useHeader && !autoSize && rows < (useFooter ? 3 : 2)) {
			throw new IllegalStateException("A fixed PagedGui does not have enough rows for its header and content");
		}
		if (this.useHeader != useHeader) {
			this.useHeader = useHeader;
			recalculateLayout(true);
		}
		return this;
	}

	public PagedGui setAutoSize(boolean autoSize) {
		if (this.autoSize != autoSize) {
			this.autoSize = autoSize;
			recalculateLayout(true);
		}
		return this;
	}

	@Override
	public PagedGui setRows(int rows) {
		validatePagedRows(rows);
		if (useHeader && rows < (useFooter ? 3 : 2)) {
			throw new IllegalArgumentException("A PagedGui does not have enough rows for its header and content");
		}
		this.autoSize = false;
		super.setRows(rows);
		recalculateLayout(true);
		return this;
	}

	public PagedGui setHeaderBackItem(ItemStack item) {
		this.headerBackItem = cloneOrNull(item);
		update();
		return this;
	}

	public PagedGui setFooterBackItem(ItemStack item) {
		this.footerBackItem = cloneOrNull(item);
		update();
		return this;
	}

	public PagedGui setPreviousPageItem(ItemStack item) {
		this.previousPageItem = cloneOrNull(item);
		update();
		return this;
	}

	public PagedGui setNextPageItem(ItemStack item) {
		this.nextPageItem = cloneOrNull(item);
		update();
		return this;
	}

	@Override
	public PagedGui setItem(int slot, ItemStack item) {
		super.setItem(slot, item);
		return this;
	}

	@Override
	public PagedGui setItem(int row, int column, ItemStack item) {
		return setItem(logicalSlot(row, column), item);
	}

	@Override
	public ItemStack getItem(int row, int column) {
		return getItem(logicalSlot(row, column));
	}

	@Override
	public PagedGui setButton(int slot, ItemStack item, Consumer<GuiClickEvent> action) {
		super.setButton(slot, item, action);
		return this;
	}

	@Override
	public PagedGui setButton(int slot, ItemStack item, ZSound sound,
							  Consumer<GuiClickEvent> action) {
		super.setButton(slot, item, sound, action);
		return this;
	}

	@Override
	public PagedGui setButton(int slot, ItemStack item, RosaSound.SoundHolder sound,
							  Consumer<GuiClickEvent> action) {
		super.setButton(slot, item, sound, action);
		return this;
	}

	@Override
	public PagedGui setButton(int slot, ItemStack item, ClickType clickType,
							  Consumer<GuiClickEvent> action) {
		super.setButton(slot, item, clickType, action);
		return this;
	}

	@Override
	public PagedGui setButton(int slot, ItemStack item, ClickType clickType, ZSound sound,
							  Consumer<GuiClickEvent> action) {
		super.setButton(slot, item, clickType, sound, action);
		return this;
	}

	@Override
	public PagedGui setButton(int slot, ItemStack item, ClickType clickType,
							  RosaSound.SoundHolder sound, Consumer<GuiClickEvent> action) {
		super.setButton(slot, item, clickType, sound, action);
		return this;
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item,
							  Consumer<GuiClickEvent> action) {
		return setButton(logicalSlot(row, column), item, action);
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item, ZSound sound,
							  Consumer<GuiClickEvent> action) {
		return setButton(logicalSlot(row, column), item, sound, action);
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item, RosaSound.SoundHolder sound,
							  Consumer<GuiClickEvent> action) {
		return setButton(logicalSlot(row, column), item, sound, action);
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item, ClickType clickType,
							  Consumer<GuiClickEvent> action) {
		return setButton(logicalSlot(row, column), item, clickType, action);
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item, ClickType clickType, ZSound sound,
							  Consumer<GuiClickEvent> action) {
		return setButton(logicalSlot(row, column), item, clickType, sound, action);
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item, ClickType clickType,
							  RosaSound.SoundHolder sound, Consumer<GuiClickEvent> action) {
		return setButton(logicalSlot(row, column), item, clickType, sound, action);
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item, ZSound sound, ClickType clickType,
							  Consumer<GuiClickEvent> action) {
		return setButton(row, column, item, clickType, sound, action);
	}

	@Override
	public PagedGui setButton(int row, int column, ItemStack item, RosaSound.SoundHolder sound,
							  ClickType clickType, Consumer<GuiClickEvent> action) {
		return setButton(row, column, item, clickType, sound, action);
	}

	@Override
	public PagedGui setAction(int row, int column, Consumer<GuiClickEvent> action) {
		super.setAction(logicalSlot(row, column), action);
		return this;
	}

	@Override
	public PagedGui setAction(int row, int column, ClickType clickType,
							  Consumer<GuiClickEvent> action) {
		super.setAction(logicalSlot(row, column), clickType, action);
		return this;
	}

	@Override
	public PagedGui clearActions(int row, int column) {
		super.clearActions(logicalSlot(row, column));
		return this;
	}

	public PagedGui setFooterItem(int column, ItemStack item) {
		int columnIndex = columnIndex(column);
		if (item == null) footerItems.remove(columnIndex);
		else footerItems.put(columnIndex, item.clone());
		update();
		return this;
	}

	public PagedGui setFooterButton(int column, ItemStack item, Consumer<GuiClickEvent> action) {
		setFooterItem(column, item);
		int actionSlot = footerActionSlot(columnIndex(column));
		setActionInternal(actionSlot, null, action);
		clearClickSoundInternal(actionSlot, null);
		return this;
	}

	public PagedGui setFooterButton(int column, ItemStack item, RosaSound.SoundHolder sound,
									Consumer<GuiClickEvent> action) {
		setFooterItem(column, item);
		int actionSlot = footerActionSlot(columnIndex(column));
		setActionInternal(actionSlot, null, action);
		setClickSoundInternal(actionSlot, null, sound);
		return this;
	}

	public PagedGui setFooterButton(int column, ItemStack item, ZSound sound,
									Consumer<GuiClickEvent> action) {
		return this.setFooterButton(column, item, soundHolder(sound), action);
	}

	public PagedGui setFooterButton(int column, ItemStack item, ClickType clickType,
									Consumer<GuiClickEvent> action) {
		setFooterItem(column, item);
		int actionSlot = footerActionSlot(columnIndex(column));
		setActionInternal(actionSlot, clickType, action);
		clearClickSoundInternal(actionSlot, clickType);
		return this;
	}

	public PagedGui setFooterButton(int column, ItemStack item, ClickType clickType, RosaSound.SoundHolder sound,
									Consumer<GuiClickEvent> action) {
		setFooterItem(column, item);
		int actionSlot = footerActionSlot(columnIndex(column));
		setActionInternal(actionSlot, clickType, action);
		setClickSoundInternal(actionSlot, clickType, sound);
		return this;
	}

	public PagedGui setFooterButton(int column, ItemStack item, ClickType clickType, ZSound sound,
									Consumer<GuiClickEvent> action) {
		return setFooterButton(column, item, clickType, soundHolder(sound), action);
	}

	public PagedGui clearFooterAction(int column) {
		int actionSlot = footerActionSlot(columnIndex(column));
		actions.remove(actionSlot);
		clickSounds.remove(actionSlot);
		return this;
	}

	public final void setPage(int page) {
		recalculateLayout(false);
		int changed = Math.max(1, Math.min(pages, page));
		if (changed == this.page) return;
		int previous = this.page;
		this.page = changed;
		update();
		onPageChange(previous, changed);
	}

	public final void nextPage() {
		setPage(page + 1);
	}

	public final void previousPage() {
		setPage(page - 1);
	}

	protected void onPageChange(int previousPage, int newPage) {
	}

	@Override
	public void reset() {
		cellItems.clear();
		actions.clear();
		clickSounds.clear();
		page = 1;
		configureNavigationActions();
		recalculateLayout(true);
	}

	@Override
	public void update() {
		recalculateLayout(false);
		if (inventory == null) return;

		int headerRows = useHeader ? 1 : 0;
		int footerStart = useFooter ? inventory.getSize() - 9 : inventory.getSize();
		int contentStart = headerRows * 9;
		int contentCapacity = footerStart - contentStart;

		if (useHeader) {
			for (int slot = 0; slot < 9; slot++) {
				ItemStack item = cellItems.get(slot);
				inventory.setItem(slot, cloneOrNull(item != null ? item : headerBackItem));
			}
		}

		int logicalStart = firstContentSlot() + (page - 1) * contentCapacity;
		for (int physical = contentStart; physical < footerStart; physical++) {
			int logical = logicalStart + (physical - contentStart);
			ItemStack item = cellItems.get(logical);
			if (item == null && !isUnlocked(physical)) item = defaultItem;
			inventory.setItem(physical, cloneOrNull(item));
		}

		if (useFooter) {
			for (int column = 0; column < 9; column++) {
				ItemStack item = footerItems.get(column);
				inventory.setItem(footerStart + column,
						cloneOrNull(item != null ? item : footerBackItem));
			}

			if (page > 1) inventory.setItem(footerStart + PREVIOUS_COLUMN, previousNavigationItem());
			if (page < pages) inventory.setItem(footerStart + NEXT_COLUMN, nextNavigationItem());
		}
	}

	@Override
	protected int resolveActionSlot(int physicalSlot) {
		int footerStart = useFooter ? getSize() - 9 : getSize();
		if (useFooter && physicalSlot >= footerStart) {
			int column = physicalSlot - footerStart;
			if (column == PREVIOUS_COLUMN && page > 1) return PREVIOUS_ACTION_SLOT;
			if (column == NEXT_COLUMN && page < pages) return NEXT_ACTION_SLOT;
			return footerActionSlot(column);
		}
		if (useHeader && physicalSlot < 9) return physicalSlot;
		int contentStart = useHeader ? 9 : 0;
		int capacity = footerStart - contentStart;
		return firstContentSlot() + (page - 1) * capacity + (physicalSlot - contentStart);
	}

	@Override
	protected void checkStoredSlot(int slot) {
		if (slot < 0) throw new IndexOutOfBoundsException("Logical slot cannot be negative: " + slot);
	}

	@Override
	protected void onContentChanged(int storedSlot) {
		recalculateLayout(true);
	}

	private void configureNavigationActions() {
		setActionInternal(PREVIOUS_ACTION_SLOT, null, event -> previousPage());
		setActionInternal(NEXT_ACTION_SLOT, null, event -> nextPage());
	}

	private void recalculateLayout(boolean refresh) {
		int oldRows = rows;
		int firstContent = firstContentSlot();
		int highest = firstContent - 1;
		for (Integer slot : cellItems.keySet()) {
			if (slot >= firstContent && slot > highest) highest = slot;
		}
		int contentCount = Math.max(0, highest - firstContent + 1);

		if (autoSize) {
			int maximumContentRows = 6 - (useHeader ? 1 : 0) - (useFooter ? 1 : 0);
			int neededRows = Math.max(1, (int) Math.ceil(contentCount / 9.0));
			int contentRows = Math.min(maximumContentRows, neededRows);
			rows = contentRows + (useHeader ? 1 : 0) + (useFooter ? 1 : 0);
		}

		int contentRows = rows - (useHeader ? 1 : 0) - (useFooter ? 1 : 0);
		if (contentRows < 1) {
			throw new IllegalStateException("PagedGui needs at least one content row");
		}
		int capacity = contentRows * 9;
		pages = Math.max(1, (int) Math.ceil(contentCount / (double) capacity));
		page = Math.max(1, Math.min(page, pages));

		if (oldRows != rows && inventory != null) {
			invalidateInventory();
		} else if (refresh) {
			update();
		}
	}

	private int firstContentSlot() {
		return useHeader ? 9 : 0;
	}

	private static int logicalSlot(int row, int column) {
		if (row < 1) throw new IndexOutOfBoundsException("Logical row must be at least 1: " + row);
		return (row - 1) * 9 + columnIndex(column);
	}

	private ItemStack previousNavigationItem() {
		if (previousPageItem == null) {
			previousPageItem = ItemCreator.of(ZMaterial.ARROW).name("&ePrevious page").make();
		}
		return previousPageItem.clone();
	}

	private ItemStack nextNavigationItem() {
		if (nextPageItem == null) {
			nextPageItem = ItemCreator.of(ZMaterial.ARROW).name("&eNext page").make();
		}
		return nextPageItem.clone();
	}

	private static int footerActionSlot(int column) {
		return FOOTER_ACTION_BASE - column;
	}

	private static int columnIndex(int column) {
		if (column < 1 || column > 9) {
			throw new IndexOutOfBoundsException("Column " + column + " outside GUI columns 1-9");
		}
		return column - 1;
	}

	private static int validatePagedRows(int rows) {
		if (rows < 2 || rows > 6) {
			throw new IllegalArgumentException("PagedGui rows must be between 2 and 6");
		}
		return rows;
	}

	private static ItemStack cloneOrNull(ItemStack item) {
		return item == null ? null : item.clone();
	}
}
