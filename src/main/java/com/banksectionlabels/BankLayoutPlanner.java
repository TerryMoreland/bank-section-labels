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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Decides which bank rows need a heading and how far each original Y must move
 * to open a gap above those rows.
 * <p>
 * Rows are groups of items that already share a Y. The vanilla bank uses a
 * 36px stride (32px item plus 4px gap) and 8 columns, but the plan uses the
 * Y values it is given, so a wider resized bank still lines up.
 */
public final class BankLayoutPlanner
{
	/** Height of a real bank item widget. Tab separator widgets are shorter. */
	public static final int ITEM_HEIGHT = 32;

	/** Vanilla distance from one item row to the next. */
	public static final int ROW_STRIDE = 36;

	private static final String TEXT_JOINER = " · ";

	private BankLayoutPlanner()
	{
	}

	/**
	 * Maps a placeholder item id to the real item, matching
	 * {@code ItemManager#canonicalize} for placeholders only.
	 */
	public interface ItemIdentity
	{
		boolean isPlaceholder(int itemId);

		/**
		 * The other form of this item, or {@code -1} when it has none.
		 * For a placeholder this is the real item id.
		 */
		int linkedId(int itemId);
	}

	public static final class ItemSlot
	{
		private final int index;
		private final int itemId;
		private final int x;
		private final int y;

		public ItemSlot(int index, int itemId, int x, int y)
		{
			this.index = index;
			this.itemId = itemId;
			this.x = x;
			this.y = y;
		}

		public int getIndex()
		{
			return index;
		}

		public int getItemId()
		{
			return itemId;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}
	}

	public static final class PlacedLabel
	{
		private final int anchorIndex;
		private final int rowY;
		private final String text;

		PlacedLabel(int anchorIndex, int rowY, String text)
		{
			this.anchorIndex = anchorIndex;
			this.rowY = rowY;
			this.text = text;
		}

		public int getAnchorIndex()
		{
			return anchorIndex;
		}

		public int getRowY()
		{
			return rowY;
		}

		public String getText()
		{
			return text;
		}
	}

	public static final class Result
	{
		private static final Result EMPTY = new Result(0, new int[0], Collections.emptyList());

		private final int headerHeight;
		private final int[] labeledRowYs;
		private final List<PlacedLabel> labels;

		private Result(int headerHeight, int[] labeledRowYs, List<PlacedLabel> labels)
		{
			this.headerHeight = headerHeight;
			this.labeledRowYs = labeledRowYs;
			this.labels = labels;
		}

		public static Result empty()
		{
			return EMPTY;
		}

		public int getExtraScroll()
		{
			return labeledRowYs.length * headerHeight;
		}

		public List<PlacedLabel> getLabels()
		{
			return labels;
		}

		/**
		 * Pixels to add to a widget whose current original Y is {@code originalY}.
		 * The labeled row itself moves down, which opens the gap above it.
		 */
		public int shiftForY(int originalY)
		{
			int rowsAboveOrAt = 0;
			for (int rowY : labeledRowYs)
			{
				if (rowY <= originalY)
				{
					rowsAboveOrAt++;
				}
				else
				{
					break;
				}
			}
			return rowsAboveOrAt * headerHeight;
		}
	}

	public static boolean shouldAdjust(boolean enabled, boolean searching, boolean tagTabOpen, boolean potionStorage)
	{
		return enabled && !searching && !tagTabOpen && !potionStorage;
	}

	public static int normalize(int itemId, ItemIdentity identity)
	{
		if (itemId < 0 || identity == null)
		{
			return itemId;
		}
		if (identity.isPlaceholder(itemId))
		{
			int linked = identity.linkedId(itemId);
			if (linked > -1 && linked != itemId)
			{
				return linked;
			}
		}
		return itemId;
	}

	/**
	 * How many earlier visible copies share this widget's item id.
	 * Returns {@code -1} when the widget is not in {@code items}.
	 */
	public static int occurrenceOf(List<ItemSlot> items, int widgetIndex, ItemIdentity identity)
	{
		List<ItemSlot> ordered = ordered(items);
		int targetNorm = -1;
		for (ItemSlot slot : ordered)
		{
			if (slot.getIndex() == widgetIndex)
			{
				targetNorm = normalize(slot.getItemId(), identity);
				break;
			}
		}
		if (targetNorm < 0)
		{
			return -1;
		}

		int seen = 0;
		for (ItemSlot slot : ordered)
		{
			if (slot.getIndex() == widgetIndex)
			{
				return seen;
			}
			if (normalize(slot.getItemId(), identity) == targetNorm)
			{
				seen++;
			}
		}
		return -1;
	}

	public static Result plan(List<ItemSlot> items, List<SectionLabel> labels, int headerHeight, ItemIdentity identity)
	{
		if (headerHeight <= 0 || items == null || items.isEmpty() || labels == null || labels.isEmpty())
		{
			return Result.empty();
		}

		TreeMap<Integer, List<ItemSlot>> rows = new TreeMap<>();
		for (ItemSlot slot : items)
		{
			rows.computeIfAbsent(slot.getY(), y -> new ArrayList<>()).add(slot);
		}
		for (List<ItemSlot> row : rows.values())
		{
			row.sort(Comparator.comparingInt(ItemSlot::getX).thenComparingInt(ItemSlot::getIndex));
		}

		Map<Integer, Integer> seen = new HashMap<>();
		List<Integer> labeledYs = new ArrayList<>();
		List<PlacedLabel> placed = new ArrayList<>();

		for (Map.Entry<Integer, List<ItemSlot>> entry : rows.entrySet())
		{
			int rowY = entry.getKey();
			List<ItemSlot> row = entry.getValue();
			List<String> texts = new ArrayList<>();
			for (ItemSlot slot : row)
			{
				int norm = normalize(slot.getItemId(), identity);
				int occurrence = seen.getOrDefault(norm, 0);
				seen.put(norm, occurrence + 1);
				for (SectionLabel label : labels)
				{
					if (label.getItemId() == norm && label.getOccurrence() == occurrence)
					{
						texts.add(label.getText());
					}
				}
			}
			if (!texts.isEmpty())
			{
				labeledYs.add(rowY);
				placed.add(new PlacedLabel(row.get(0).getIndex(), rowY, String.join(TEXT_JOINER, texts)));
			}
		}

		if (labeledYs.isEmpty())
		{
			return Result.empty();
		}

		int[] ys = new int[labeledYs.size()];
		for (int i = 0; i < labeledYs.size(); i++)
		{
			ys[i] = labeledYs.get(i);
		}
		return new Result(headerHeight, ys, placed);
	}

	private static List<ItemSlot> ordered(List<ItemSlot> items)
	{
		List<ItemSlot> ordered = new ArrayList<>(items);
		ordered.sort(Comparator
			.comparingInt(ItemSlot::getY)
			.thenComparingInt(ItemSlot::getX)
			.thenComparingInt(ItemSlot::getIndex));
		return ordered;
	}
}
