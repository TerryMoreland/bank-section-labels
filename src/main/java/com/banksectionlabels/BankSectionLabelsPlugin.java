/*
 * Copyright (c) 2026 Bank Section Labels contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.banksectionlabels;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Each section is its own grid of item ids, including empty cells. Widgets are
 * moved with {@code setOriginalX}, {@code setOriginalY}, and {@code revalidate}.
 * Their bank-slot indexes stay the same, so withdraw and deposit still hit the
 * real item. Drops onto a section cell, including an empty one, update the
 * saved grid instead of the vanilla bank order.
 */
@Slf4j
@PluginDescriptor(
	name = "Bank Section Labels",
	description = "Draw your own headings above rows of items in regular bank tabs",
	tags = {"bank", "labels", "sections", "headers"}
)
@PluginDependency(BankTagsPlugin.class)
public class BankSectionLabelsPlugin extends Plugin
{
	public static final String CONFIG_GROUP = "banksectionlabels";
	static final String LABELS_KEY = "labels";

	/**
	 * Potion storage is presented as a fake bank tab. It is not a normal tab,
	 * and labels are not applied there.
	 */
	private static final int POTION_STORAGE_TAB = 15;

	private static final String ADD_LABEL = "Add section label";
	private static final String EDIT_LABEL = "Edit section label";
	private static final String REMOVE_SECTION = "Remove section";
	private static final String REMOVE_FROM_SECTION = "Remove from section";
	private static final String RESET_TAB = "Reset section labels for this tab";
	private static final String RENAME_TAB = "Rename tab title";
	private static final String EXAMINE = "Examine";
	private static final String VIEW_TAB = "View tab";
	private static final String PROXY_NAME = "bsl-cell";
	private static final String HEADING_NAME = "bsl-heading";
	/** Bank tab buttons are children 10–19 of the tab container (0 = all items). */
	private static final int TAB_CHILD_START = 10;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BankSectionLabelsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private BankSearch bankSearch;

	@Inject
	private BankTagsService bankTagsService;

	@Inject
	private BankSectionLabelsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private int layoutGeneration;
	private int layoutColumns = 8;
	private long handledDropNanos;
	private final List<Widget> proxies = new ArrayList<>();
	private final Map<Widget, int[]> proxyCells = new HashMap<>();
	private final Map<Integer, int[]> cellByBankIndex = new HashMap<>();
	private final List<SectionLayout.Placed> hitSlots = new ArrayList<>();
	private List<SectionLayout.Section> prepared = new ArrayList<>();

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		log.info("Bank Section Labels started");
		relayout();
	}

	@Override
	protected void shutDown()
	{
		overlay.setSections(Collections.emptyList());
		overlayManager.remove(overlay);
		log.info("Bank Section Labels stopped");
		// This plugin is already unregistered, so the rebuild restores vanilla positions.
		relayout();
	}

	@Provides
	BankSectionLabelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankSectionLabelsConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("resetSections".equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			if (clientThread != null)
			{
				clientThread.invoke(this::resetOpenTab);
			}
			return;
		}
		relayout();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		relayout();
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN && event.isUnload())
		{
			overlay.setSections(Collections.emptyList());
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event == null || config == null || !config.enabled() || isPotionStorage())
		{
			return;
		}
		if (offerTabRename(event))
		{
			return;
		}
		if (event.getActionParam1() != InterfaceID.Bankmain.ITEMS || !EXAMINE.equals(event.getOption()))
		{
			return;
		}
		if (!BankLayoutPlanner.shouldAdjust(config.enabled(), isSearching(), isTagTabOpen(), isPotionStorage()))
		{
			return;
		}
		if (config.requireShift() && !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null)
		{
			return;
		}
		Widget item = container.getChild(event.getActionParam0());
		if (!isItemWidget(item))
		{
			return;
		}

		int[] placed = cellByBankIndex.get(item.getIndex());
		String target = event.getTarget();
		int viewTab = currentTab();
		int owner = viewTab == 0 ? SectionLayout.tabForSlot(item.getIndex(), readTabCounts()) : viewTab;
		int itemId = normalizeId(item.getItemId());
		if (itemId < 0)
		{
			return;
		}

		final int resetTab = placed != null ? placed[0] : owner;
		addMenuEntry(RESET_TAB, target, () -> resetTab(resetTab));
		if (owner >= 1 && owner <= 9)
		{
			addMenuEntry(RENAME_TAB, target, () -> promptTabTitle(owner));
		}

		if (placed == null || placed.length < 3 || placed[1] < 0)
		{
			addMenuEntry(ADD_LABEL, target, () -> promptForLabel(owner, itemId, ""));
			return;
		}
		final int sectionTab = placed[0];
		final int sectionIndex = placed[1];
		final int cellIndex = placed[2];
		if (cellIndex == 0)
		{
			String name = sectionName(sectionTab, sectionIndex);
			addMenuEntry(REMOVE_SECTION, target, () ->
			{
				List<SectionLayout.Section> tabSections = SectionLayout.forTab(prepared, sectionTab);
				saveOneTab(sectionTab, SectionLayout.removeSection(tabSections, sectionIndex));
				relayout();
			});
			addMenuEntry(EDIT_LABEL, target, () -> promptForLabel(sectionTab, itemId, name));
		}
		else
		{
			addMenuEntry(REMOVE_FROM_SECTION, target, () ->
			{
				List<SectionLayout.Section> tabSections = SectionLayout.forTab(prepared, sectionTab);
				saveOneTab(sectionTab, SectionLayout.clearCell(tabSections, sectionIndex, cellIndex));
				relayout();
			});
		}
	}

	/**
	 * Runs after the bank build, including Bank Tags' layout pass, has placed
	 * every item. Lower priority runs later than the default subscribers.
	 */
	@Subscribe(priority = -1)
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.BANKMAIN_FINISHBUILDING)
		{
			return;
		}

		int generation = ++layoutGeneration;
		try
		{
			Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
			if (container == null || !layoutAllowed())
			{
				clearLayoutVisuals();
				if (container != null)
				{
					hideProxies(container.getChildren());
				}
				return;
			}
			applyLayout(container);
			clientThread.invokeLater(() -> updateScrollbar(generation));
		}
		catch (RuntimeException ex)
		{
			log.warn("Bank section layout failed", ex);
			clearLayoutVisuals();
		}
	}

	private void applyLayout(Widget container)
	{
		if (container == null)
		{
			return;
		}
		Widget[] children = container.getChildren();
		if (children == null)
		{
			clearLayoutVisuals();
			return;
		}

		hideProxies(children);
		List<SectionLayout.Section> loaded = loadSections();
		List<SectionLayout.Section> all = SectionLayout.prepare(loaded);
		if (!SectionLayout.encode(all).equals(SectionLayout.encode(loaded)))
		{
			saveSections(all);
		}
		prepared = all;

		int tab = currentTab();
		if (tab != 0 && SectionLayout.forTab(all, tab).isEmpty())
		{
			clearLayoutVisuals();
			return;
		}

		List<Widget> items = new ArrayList<>();
		List<Widget> separators = new ArrayList<>();
		Map<Integer, Widget> byIndex = new HashMap<>();
		for (Widget child : children)
		{
			if (child == null || isOurWidget(child))
			{
				continue;
			}
			if (isItemWidget(child))
			{
				items.add(child);
				byIndex.put(child.getIndex(), child);
			}
			else if (!child.isSelfHidden())
			{
				separators.add(child);
			}
		}
		items.sort(Comparator.comparingInt(Widget::getIndex));
		separators.sort(Comparator.comparingInt(Widget::getOriginalY));

		layoutColumns = detectColumns(items, container);
		SectionLayout.Spec spec = new SectionLayout.Spec(
			layoutColumns,
			Math.max(1, config.headerHeight()),
			BankLayoutPlanner.START_X,
			BankLayoutPlanner.COLUMN_STRIDE,
			BankLayoutPlanner.ROW_STRIDE);

		List<SectionLayout.BankItem> bankItems = new ArrayList<>();
		for (Widget item : items)
		{
			bankItems.add(new SectionLayout.BankItem(item.getIndex(), normalizeId(item.getItemId())));
		}

		SectionLayout.BankView view;
		if (tab == 0)
		{
			view = SectionLayout.layoutAll(bankItems, readTabCounts(), normalized(all), spec, config.showTabTitles());
		}
		else
		{
			view = SectionLayout.layoutTab(bankItems, tab, normalized(SectionLayout.forTab(all, tab)), spec);
			for (Widget separator : separators)
			{
				separator.setHidden(true);
			}
		}

		cellByBankIndex.clear();
		hitSlots.clear();
		for (SectionLayout.Placed placed : view.getPlaced())
		{
			if (placed.isEmpty())
			{
				createProxy(container, placed);
				if (placed.inSection())
				{
					hitSlots.add(placed);
				}
				continue;
			}
			Widget widget = byIndex.get(placed.getBankIndex());
			if (widget == null)
			{
				continue;
			}
			placeWidget(widget, placed.getX(), placed.getY());
			armDrag(widget);
			if (placed.inSection())
			{
				cellByBankIndex.put(widget.getIndex(), new int[]{placed.getTab(), placed.getSectionIndex(), placed.getCellIndex()});
				hitSlots.add(placed);
			}
		}

		if (tab == 0)
		{
			List<Integer> lines = view.getSeparatorY();
			for (int i = 0; i < separators.size(); i++)
			{
				Widget separator = separators.get(i);
				if (i < lines.size())
				{
					separator.setHidden(false);
					if (separator.getOriginalY() != lines.get(i))
					{
						separator.setOriginalY(lines.get(i));
						separator.revalidate();
					}
				}
				else
				{
					separator.setHidden(true);
				}
			}
		}

		int bottom = view.getContentBottom() + BankLayoutPlanner.ROW_STRIDE;
		int viewport = container.getHeight();
		container.setScrollHeight(Math.max(bottom, viewport));

		int headingWidth = layoutColumns * BankLayoutPlanner.COLUMN_STRIDE;
		List<BankSectionLabelsOverlay.SectionDraw> drawn = new ArrayList<>();
		if (config.showTabTitles())
		{
			for (SectionLayout.TabHeading heading : view.getTabHeadings())
			{
				final int titledTab = heading.getTab();
				String title = TabTitles.resolve(configManager, titledTab);
				drawn.add(new BankSectionLabelsOverlay.SectionDraw(
					heading.getY(),
					heading.getHeight(),
					heading.getX(),
					title,
					true));
				createHeadingWidget(container, heading.getX(), heading.getY(), headingWidth, heading.getHeight(), RENAME_TAB, () -> promptTabTitle(titledTab));
			}
		}
		for (SectionLayout.Header header : view.getHeaders())
		{
			if (header == null || header.getName() == null || header.getName().isEmpty())
			{
				continue;
			}
			final int sectionTab = header.getTab();
			final int sectionIndex = header.getSectionIndex();
			drawn.add(new BankSectionLabelsOverlay.SectionDraw(header.getY(), header.getHeight(), header.getX(), header.getName(), false));
			createHeadingWidget(container, header.getX(), header.getY(), headingWidth, header.getHeight(), EDIT_LABEL, () -> promptSectionName(sectionTab, sectionIndex));
		}
		overlay.setSections(drawn);
		log.debug("Placed {} heading(s) on bank tab {}", drawn.size(), tab);
	}

	private List<SectionLayout.Section> normalized(List<SectionLayout.Section> sections)
	{
		List<SectionLayout.Section> out = new ArrayList<>();
		if (sections == null)
		{
			return out;
		}
		for (SectionLayout.Section section : sections)
		{
			List<Integer> cells = new ArrayList<>();
			for (Integer id : section.getCells())
			{
				if (id == null || id < 0)
				{
					cells.add(SectionLayout.EMPTY);
				}
				else
				{
					cells.add(normalizeId(id));
				}
			}
			out.add(new SectionLayout.Section(section.getTab(), section.getColumns(), section.getName(), cells));
		}
		return out;
	}

	private void updateScrollbar(int generation)
	{
		try
		{
			if (generation != layoutGeneration || client == null)
			{
				return;
			}
			Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
			if (container == null || container.isHidden())
			{
				return;
			}

			int scrollY = container.getScrollY();
			int height = container.getHeight();
			int maxScroll = Math.max(0, container.getScrollHeight() - height);
			if (scrollY > maxScroll)
			{
				scrollY = maxScroll;
				container.setScrollY(scrollY);
				client.setVarcIntValue(VarClientID.BANK_SCROLLPOS, scrollY);
			}

			client.runScript(
				ScriptID.UPDATE_SCROLLBAR,
				InterfaceID.Bankmain.SCROLLBAR,
				InterfaceID.Bankmain.ITEMS,
				scrollY
			);
		}
		catch (RuntimeException ex)
		{
			log.debug("Could not update the bank scrollbar", ex);
		}
	}

	private boolean offerTabRename(MenuEntryAdded event)
	{
		String option = Text.removeTags(event.getOption());
		if (!VIEW_TAB.equals(option))
		{
			return false;
		}
		int tab;
		try
		{
			tab = Integer.parseInt(Text.removeTags(event.getTarget()).trim());
		}
		catch (NumberFormatException ex)
		{
			return false;
		}
		if (tab < 1 || tab > 9)
		{
			return false;
		}
		final int titled = tab;
		addMenuEntry(RENAME_TAB, event.getTarget(), () -> promptTabTitle(titled));
		return true;
	}

	private void promptTabTitle(int tab)
	{
		if (chatboxPanelManager == null || tab < 1 || tab > 9)
		{
			return;
		}
		String current = TabTitles.resolve(configManager, tab);
		chatboxPanelManager.openTextInput("Tab " + tab + " title (empty clears your name)")
			.value(current == null ? "" : current)
			.onDone(value ->
			{
				if (clientThread == null)
				{
					return;
				}
				clientThread.invoke(() -> applyTabTitle(tab, value));
			})
			.build();
	}

	private void applyTabTitle(int tab, String value)
	{
		try
		{
			java.util.Map<Integer, String> titles = TabTitles.withTitle(TabTitles.decode(TabTitles.readRaw(configManager)), tab, value);
			TabTitles.save(configManager, titles);
			relayout();
		}
		catch (RuntimeException ex)
		{
			log.warn("Could not save tab title", ex);
		}
	}

	private void promptSectionName(int tab, int sectionIndex)
	{
		if (chatboxPanelManager == null)
		{
			return;
		}
		String current = sectionName(tab, sectionIndex);
		chatboxPanelManager.openTextInput("Section label (leave empty to remove)")
			.value(current)
			.onDone(value ->
			{
				if (clientThread == null)
				{
					return;
				}
				clientThread.invoke(() -> applySectionName(tab, sectionIndex, value));
			})
			.build();
	}

	private void applySectionName(int tab, int sectionIndex, String value)
	{
		try
		{
			List<SectionLayout.Section> all = SectionLayout.prepare(loadSections());
			List<SectionLayout.Section> tabSections = SectionLayout.forTab(all, tab);
			String text = SectionLabels.sanitize(value);
			if (text.isEmpty())
			{
				tabSections = SectionLayout.removeSection(tabSections, sectionIndex);
			}
			else
			{
				tabSections = SectionLayout.rename(tabSections, sectionIndex, text);
			}
			prepared = SectionLayout.replaceTab(all, tab, tabSections);
			saveSections(prepared);
			relayout();
		}
		catch (RuntimeException ex)
		{
			log.warn("Could not rename section", ex);
		}
	}

	private Widget tabButton(int tab)
	{
		if (client == null || tab < 1 || tab > 9)
		{
			return null;
		}
		Widget tabs = client.getWidget(InterfaceID.Bankmain.TABS);
		if (tabs == null)
		{
			return null;
		}
		Widget button = tabs.getChild(TAB_CHILD_START + tab);
		if (button == null || button.isHidden())
		{
			return null;
		}
		return button;
	}

	private void createHeadingWidget(Widget container, int x, int y, int width, int height, String action, Runnable onClick)
	{
		if (container == null || onClick == null || width <= 0 || height <= 0)
		{
			return;
		}
		Widget heading = container.createChild(-1, WidgetType.RECTANGLE);
		if (heading == null)
		{
			return;
		}
		heading.setName(HEADING_NAME);
		heading.setOriginalX(x);
		heading.setOriginalY(y);
		heading.setOriginalWidth(width);
		heading.setOriginalHeight(Math.max(1, height - 2));
		heading.setOpacity(255);
		heading.setFilled(false);
		heading.setHidden(false);
		heading.setHasListener(true);
		heading.setNoClickThrough(true);
		heading.setAction(0, action);
		heading.setOnOpListener((JavaScriptCallback) ev -> onClick.run());
		heading.revalidate();
		proxies.add(heading);
	}

	private static boolean isOurWidget(Widget widget)
	{
		if (widget == null || widget.getName() == null)
		{
			return false;
		}
		return widget.getName().startsWith("bsl-");
	}

	private void promptForLabel(int tab, int itemId, String initial)
	{
		if (chatboxPanelManager == null)
		{
			return;
		}
		final int columns = layoutColumns;
		chatboxPanelManager.openTextInput("Section label (leave empty to remove)")
			.value(initial == null ? "" : initial)
			.onDone(value ->
			{
				// Chatbox onDone is not the client thread. Item lookups and the
				// bank widgets have to run there.
				if (clientThread == null)
				{
					return;
				}
				clientThread.invoke(() -> applyLabel(tab, itemId, columns, value));
			})
			.build();
	}

	private void applyLabel(int tab, int itemId, int columns, String value)
	{
		try
		{
			List<SectionLayout.Section> all = SectionLayout.prepare(loadSections());
			List<SectionLayout.Section> tabSections = SectionLayout.forTab(all, tab);
			tabSections = SectionLayout.labelItem(tabSections, tab, value, columns, itemId);
			prepared = SectionLayout.replaceTab(all, tab, tabSections);
			saveSections(prepared);
			relayout();
		}
		catch (RuntimeException ex)
		{
			log.warn("Could not save section label", ex);
		}
	}

	private void onDragComplete(ScriptEvent event)
	{
		try
		{
			if (event == null || client == null || !layoutAllowed())
			{
				return;
			}
			Widget source = event.getSource();
			if (source == null)
			{
				source = client.getDraggedWidget();
			}
			if (!isItemWidget(source))
			{
				return;
			}
			int[] destination = destinationCell(event.getTarget() != null ? event.getTarget() : client.getDraggedOnWidget());
			if (destination == null)
			{
				Widget landed = client.getDraggedOnWidget();
				if (landed != null && isOurWidget(landed))
				{
					client.setDraggedOnWidget(null);
				}
				return;
			}
			int viewTab = currentTab();
			int sourceTab = viewTab == 0 ? SectionLayout.tabForSlot(source.getIndex(), readTabCounts()) : viewTab;
			int destTab = destination[0];
			long now = System.nanoTime();
			if (now - handledDropNanos < 200_000_000L)
			{
				client.setDraggedOnWidget(null);
				return;
			}
			handledDropNanos = now;

			int[] sourceCell = cellByBankIndex.get(source.getIndex());
			int sourceSection = sourceCell == null || sourceCell.length < 3 || sourceCell[0] != sourceTab ? -1 : sourceCell[1];
			int sourceIndex = sourceCell == null || sourceCell.length < 3 || sourceCell[0] != sourceTab ? -1 : sourceCell[2];
			int sourceItem = normalizeId(source.getItemId());
			if (sourceItem < 0)
			{
				return;
			}

			boolean moveTab = SectionLayout.movesVanillaTab(sourceTab, destTab);
			if (moveTab)
			{
				Widget tabButton = tabButton(destTab);
				if (tabButton != null)
				{
					// The client sends the normal drag-onto-tab packet from whatever
					// widget is dragged on when this listener returns.
					client.setDraggedOnWidget(tabButton);
				}
				else
				{
					client.setDraggedOnWidget(null);
					log.warn("Could not find bank tab {} to move the item into", destTab);
				}
			}
			else
			{
				client.setDraggedOnWidget(null);
			}

			prepared = SectionLayout.placeItem(prepared, sourceTab, sourceSection, sourceIndex, sourceItem, destTab, destination[1], destination[2]);
			saveSections(prepared);
			if (!moveTab)
			{
				relayout();
			}
		}
		catch (RuntimeException ex)
		{
			log.warn("Section drop failed", ex);
		}
	}

	private int[] destinationCell(Widget target)
	{
		if (target != null)
		{
			int[] proxy = proxyCells.get(target);
			if (proxy != null)
			{
				return proxy;
			}
			if (isItemWidget(target))
			{
				int[] placed = cellByBankIndex.get(target.getIndex());
				if (placed != null)
				{
					return placed;
				}
			}
		}
		if (client == null || hitSlots.isEmpty())
		{
			return null;
		}
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		net.runelite.api.Point origin = container == null ? null : container.getCanvasLocation();
		if (container == null || mouse == null || origin == null)
		{
			return null;
		}
		int localX = SectionLayout.contentX(mouse.getX(), origin.getX());
		int localY = SectionLayout.contentY(mouse.getY(), origin.getY(), container.getScrollY());
		return SectionLayout.hitTest(hitSlots, localX, localY);
	}

	private void addMenuEntry(String option, String target, Runnable action)
	{
		if (client == null || client.getMenu() == null || action == null)
		{
			return;
		}
		client.getMenu().createMenuEntry(-1)
			.setOption(option)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(entry -> action.run());
	}

	private List<BankLayoutPlanner.ItemSlot> visibleItems(Widget container)
	{
		Widget[] children = container.getChildren();
		if (children == null)
		{
			return Collections.emptyList();
		}

		List<BankLayoutPlanner.ItemSlot> slots = new ArrayList<>();
		for (Widget child : children)
		{
			if (!isItemWidget(child))
			{
				continue;
			}
			slots.add(new BankLayoutPlanner.ItemSlot(
				child.getIndex(),
				child.getItemId(),
				child.getOriginalX(),
				child.getOriginalY()
			));
		}
		return slots;
	}

	private static boolean isItemWidget(Widget widget)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return false;
		}
		int itemId = widget.getItemId();
		if (itemId < 0 || itemId == ItemID.BLANKOBJECT)
		{
			return false;
		}
		return widget.getOriginalHeight() == BankLayoutPlanner.ITEM_HEIGHT;
	}

	private BankLayoutPlanner.ItemIdentity identity()
	{
		return new BankLayoutPlanner.ItemIdentity()
		{
			@Override
			public boolean isPlaceholder(int itemId)
			{
				ItemComposition composition = itemManager.getItemComposition(itemId);
				return composition.getPlaceholderTemplateId() != -1;
			}

			@Override
			public int linkedId(int itemId)
			{
				return itemManager.getItemComposition(itemId).getPlaceholderId();
			}
		};
	}

	private List<SectionLayout.Section> loadSections()
	{
		if (configManager == null)
		{
			return new ArrayList<>();
		}
		return SectionLayout.decode(configManager.getRSProfileConfiguration(CONFIG_GROUP, LABELS_KEY));
	}

	private void saveSections(List<SectionLayout.Section> sections)
	{
		if (configManager == null)
		{
			return;
		}
		if (sections == null || sections.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(CONFIG_GROUP, LABELS_KEY);
			return;
		}
		configManager.setRSProfileConfiguration(CONFIG_GROUP, LABELS_KEY, SectionLayout.encode(sections));
	}

	private void saveOneTab(int tab, List<SectionLayout.Section> updatedTab)
	{
		prepared = SectionLayout.replaceTab(prepared == null || prepared.isEmpty() ? loadSections() : prepared, tab, updatedTab);
		saveSections(prepared);
	}

	private void resetOpenTab()
	{
		try
		{
			resetTab(currentTab());
			if (configManager != null)
			{
				configManager.setConfiguration(CONFIG_GROUP, "resetSections", false);
			}
		}
		catch (RuntimeException ex)
		{
			log.warn("Could not reset section labels", ex);
		}
	}

	private void resetTab(int tab)
	{
		List<SectionLayout.Section> all = SectionLayout.prepare(loadSections());
		prepared = SectionLayout.replaceTab(all, tab, Collections.emptyList());
		saveSections(prepared);
		relayout();
	}

	private String sectionName(int tab, int sectionIndex)
	{
		List<SectionLayout.Section> tabSections = SectionLayout.forTab(prepared, tab);
		if (sectionIndex < 0 || sectionIndex >= tabSections.size())
		{
			return "";
		}
		return tabSections.get(sectionIndex).getName();
	}

	private int[] readTabCounts()
	{
		int[] counts = new int[9];
		if (client == null)
		{
			return counts;
		}
		int[] varbits = {
			VarbitID.BANK_TAB_1,
			VarbitID.BANK_TAB_2,
			VarbitID.BANK_TAB_3,
			VarbitID.BANK_TAB_4,
			VarbitID.BANK_TAB_5,
			VarbitID.BANK_TAB_6,
			VarbitID.BANK_TAB_7,
			VarbitID.BANK_TAB_8,
			VarbitID.BANK_TAB_9
		};
		for (int i = 0; i < varbits.length; i++)
		{
			counts[i] = Math.max(0, client.getVarbitValue(varbits[i]));
		}
		return counts;
	}

	private void relayout()
	{
		if (clientThread == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			try
			{
				if (client == null || bankSearch == null)
				{
					return;
				}
				Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
				if (container == null || container.isHidden())
				{
					return;
				}
				bankSearch.layoutBank();
			}
			catch (RuntimeException ex)
			{
				log.debug("Could not rebuild the bank layout", ex);
			}
		});
	}

	private int currentTab()
	{
		return client.getVarbitValue(VarbitID.BANK_CURRENTTAB);
	}

	private boolean isPotionStorage()
	{
		return currentTab() == POTION_STORAGE_TAB;
	}

	private boolean isSearching()
	{
		// Only the live search-input mode. A closed chatbox can leave text in
		// MESLAYERINPUT (including a heading the user just typed), and that must
		// not be treated as bank search.
		if (client == null)
		{
			return false;
		}
		return client.getVarcIntValue(VarClientID.MESLAYERMODE) == InputType.SEARCH.getType();
	}

	private boolean isTagTabOpen()
	{
		try
		{
			if (bankTagsService == null)
			{
				return false;
			}
			String tag = bankTagsService.getActiveTag();
			return (tag != null && !tag.isEmpty()) || bankTagsService.getActiveLayout() != null;
		}
		catch (RuntimeException ex)
		{
			return false;
		}
	}

	private boolean layoutAllowed()
	{
		try
		{
			return config != null && BankLayoutPlanner.shouldAdjust(config.enabled(), isSearching(), isTagTabOpen(), isPotionStorage());
		}
		catch (RuntimeException ex)
		{
			return false;
		}
	}

	private void clearLayoutVisuals()
	{
		cellByBankIndex.clear();
		hitSlots.clear();
		if (overlay != null)
		{
			overlay.setSections(Collections.emptyList());
		}
	}

	private void hideProxies(Widget[] children)
	{
		if (children != null)
		{
			for (Widget child : children)
			{
				if (child != null && isOurWidget(child))
				{
					child.setHidden(true);
				}
			}
		}
		for (Widget proxy : proxies)
		{
			try
			{
				if (proxy != null)
				{
					proxy.setHidden(true);
				}
			}
			catch (RuntimeException ignored)
			{
				// The bank build already destroyed this proxy.
			}
		}
		proxies.clear();
		proxyCells.clear();
	}

	private void createProxy(Widget container, SectionLayout.Placed slot)
	{
		if (container == null || slot == null)
		{
			return;
		}
		Widget proxy = container.createChild(-1, WidgetType.RECTANGLE);
		if (proxy == null)
		{
			return;
		}
		proxy.setName(PROXY_NAME);
		proxy.setOriginalX(slot.getX());
		proxy.setOriginalY(slot.getY());
		proxy.setOriginalWidth(BankLayoutPlanner.COLUMN_STRIDE);
		proxy.setOriginalHeight(BankLayoutPlanner.ROW_STRIDE);
		proxy.setOpacity(255);
		proxy.setFilled(false);
		proxy.setHidden(false);
		proxy.setHasListener(true);
		proxy.setNoClickThrough(true);
		proxy.setDragParent(container);
		proxy.setOnDragCompleteListener((JavaScriptCallback) this::onDragComplete);
		proxy.revalidate();
		proxies.add(proxy);
		proxyCells.put(proxy, new int[]{slot.getTab(), slot.getSectionIndex(), slot.getCellIndex()});
	}

	private void placeWidget(Widget widget, int x, int y)
	{
		if (widget == null)
		{
			return;
		}
		if (widget.getOriginalX() != x || widget.getOriginalY() != y)
		{
			widget.setOriginalX(x);
			widget.setOriginalY(y);
			widget.revalidate();
		}
	}

	private void armDrag(Widget widget)
	{
		if (widget == null)
		{
			return;
		}
		widget.setOnDragCompleteListener((JavaScriptCallback) this::onDragComplete);
	}

	private int detectColumns(List<Widget> items, Widget container)
	{
		int fullest = 1;
		Map<Integer, Integer> rows = new HashMap<>();
		if (items != null)
		{
			for (Widget item : items)
			{
				if (item == null)
				{
					continue;
				}
				int count = rows.getOrDefault(item.getOriginalY(), 0) + 1;
				rows.put(item.getOriginalY(), count);
				if (count > fullest)
				{
					fullest = count;
				}
			}
		}
		int columns = Math.max(8, fullest);
		if (container != null)
		{
			int guess = (container.getWidth() - BankLayoutPlanner.START_X) / BankLayoutPlanner.COLUMN_STRIDE;
			if (guess >= 10 && guess <= 16)
			{
				columns = Math.max(columns, guess);
			}
		}
		return columns;
	}

	private int normalizeId(int itemId)
	{
		try
		{
			return BankLayoutPlanner.normalize(itemId, identity());
		}
		catch (RuntimeException ex)
		{
			return itemId;
		}
	}

}
