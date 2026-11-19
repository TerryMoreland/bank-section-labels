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
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptID;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
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
 * Starts each labeled item on its own row in the vanilla bank, then draws the
 * heading in the gap above that row.
 * <p>
 * Widget X and Y are changed with {@code setOriginalX}, {@code setOriginalY},
 * and {@code revalidate}, the same approach Bank Tags uses to place items.
 * Child indexes, item ids, and menu actions are left alone, so a drag, withdraw,
 * or deposit still hits the real bank slot of the item you see. The scroll
 * height grows so the last rows stay reachable.
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
	private static final String REMOVE_LABEL = "Remove section label";
	private static final String EXAMINE = "Examine";

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
		if (CONFIG_GROUP.equals(event.getGroup()))
		{
			relayout();
		}
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

		List<BankLayoutPlanner.ItemSlot> slots = visibleItems(container);
		int occurrence = BankLayoutPlanner.occurrenceOf(slots, item.getIndex(), identity());
		if (occurrence < 0)
		{
			return;
		}

		int tab = currentTab();
		int itemId = BankLayoutPlanner.normalize(item.getItemId(), identity());
		String existing = SectionLabels.textFor(loadLabels(), tab, itemId, occurrence);
		String target = event.getTarget();

		if (existing == null)
		{
			addMenuEntry(ADD_LABEL, target, () -> promptForLabel(tab, itemId, occurrence, ""));
		}
		else
		{
			addMenuEntry(REMOVE_LABEL, target, () ->
			{
				saveLabels(SectionLabels.remove(loadLabels(), tab, itemId, occurrence));
				relayout();
			});
			addMenuEntry(EDIT_LABEL, target, () -> promptForLabel(tab, itemId, occurrence, existing));
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
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null || !BankLayoutPlanner.shouldAdjust(config.enabled(), isSearching(), isTagTabOpen(), isPotionStorage()))
		{
			overlay.setSections(Collections.emptyList());
			return;
		}

		applySpacing(container);
		clientThread.invokeLater(() -> updateScrollbar(generation));
	}

	private void applySpacing(Widget container)
	{
		Widget[] children = container.getChildren();
		if (children == null)
		{
			overlay.setSections(Collections.emptyList());
			return;
		}

		List<BankLayoutPlanner.ItemSlot> slots = visibleItems(container);
		List<SectionLabel> labels = SectionLabels.forTab(loadLabels(), currentTab());
		BankLayoutPlanner.Result plan = BankLayoutPlanner.plan(slots, labels, config.headerHeight(), identity(), container.getWidth());
		if (plan.isEmpty())
		{
			overlay.setSections(Collections.emptyList());
			return;
		}

		for (Widget child : children)
		{
			if (child == null || child.isSelfHidden())
			{
				continue;
			}
			BankLayoutPlanner.Placement placement = plan.placementFor(child.getIndex());
			if (placement != null)
			{
				if (child.getOriginalX() != placement.getX() || child.getOriginalY() != placement.getY())
				{
					child.setOriginalX(placement.getX());
					child.setOriginalY(placement.getY());
					child.revalidate();
				}
				continue;
			}
			if (isItemWidget(child))
			{
				continue;
			}
			int separatorY = plan.yForNonItem(child.getOriginalY(), child.getOriginalHeight());
			if (separatorY != child.getOriginalY())
			{
				child.setOriginalY(separatorY);
				child.revalidate();
			}
		}

		int bottom = 0;
		for (Widget child : children)
		{
			if (child == null || child.isSelfHidden())
			{
				continue;
			}
			bottom = Math.max(bottom, child.getOriginalY() + child.getOriginalHeight());
		}
		// Keep the vanilla 4px row padding, plus a little room under the last row.
		bottom += BankLayoutPlanner.ROW_STRIDE - BankLayoutPlanner.ITEM_HEIGHT + 4;
		container.setScrollHeight(Math.max(bottom, container.getHeight()));

		List<BankSectionLabelsOverlay.SectionDraw> drawn = new ArrayList<>();
		for (BankLayoutPlanner.PlacedLabel label : plan.getLabels())
		{
			Widget anchor = container.getChild(label.getAnchorIndex());
			if (anchor != null)
			{
				drawn.add(new BankSectionLabelsOverlay.SectionDraw(anchor, label.getText()));
			}
		}
		overlay.setSections(drawn);
		log.debug("Placed {} section label(s) on bank tab {}", drawn.size(), currentTab());
	}

	private void updateScrollbar(int generation)
	{
		if (generation != layoutGeneration)
		{
			return;
		}
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null)
		{
			return;
		}

		int scrollY = container.getScrollY();
		int maxScroll = Math.max(0, container.getScrollHeight() - container.getHeight());
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

	private void promptForLabel(int tab, int itemId, int occurrence, String initial)
	{
		chatboxPanelManager.openTextInput("Section label (leave empty to remove)")
			.value(initial == null ? "" : initial)
			.onDone(value ->
			{
				String text = SectionLabels.sanitize(value);
				List<SectionLabel> labels = loadLabels();
				if (text.isEmpty())
				{
					labels = SectionLabels.remove(labels, tab, itemId, occurrence);
				}
				else
				{
					labels = SectionLabels.upsert(labels, new SectionLabel(tab, itemId, occurrence, text));
				}
				saveLabels(labels);
				relayout();
			})
			.build();
	}

	private void addMenuEntry(String option, String target, Runnable action)
	{
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

	private List<SectionLabel> loadLabels()
	{
		return SectionLabels.decode(configManager.getRSProfileConfiguration(CONFIG_GROUP, LABELS_KEY));
	}

	private void saveLabels(List<SectionLabel> labels)
	{
		if (labels.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(CONFIG_GROUP, LABELS_KEY);
			return;
		}
		configManager.setRSProfileConfiguration(CONFIG_GROUP, LABELS_KEY, SectionLabels.encode(labels));
	}

	private void relayout()
	{
		clientThread.invokeLater(() ->
		{
			Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
			if (container == null || container.isHidden())
			{
				return;
			}
			bankSearch.layoutBank();
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
		return client.getVarcIntValue(VarClientID.MESLAYERMODE) == InputType.SEARCH.getType();
	}

	private boolean isTagTabOpen()
	{
		String tag = bankTagsService.getActiveTag();
		return (tag != null && !tag.isEmpty()) || bankTagsService.getActiveLayout() != null;
	}

}
