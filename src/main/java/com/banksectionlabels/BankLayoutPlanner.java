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
 * Reflows a vanilla bank tab so each labeled item starts a new row.
 * <p>
 * Items stay in their real bank order. A section head that is not already in
 * the first column finishes the previous row early (the rest of that row stays
 * empty), then a header gap is inserted and the head is placed at column 0.
 * Later items fill that row and the rows under it until the next head.
 */
public final class BankLayoutPlanner
{
	/** Height of a real bank item widget. Tab separator widgets are shorter. */
	public static final int ITEM_HEIGHT = 32;

	/** Vanilla distance from one item row to the next. */
	public static final int ROW_STRIDE = 36;

	/** Vanilla distance from one column to the next. */
	public static final int COLUMN_STRIDE = 48;

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

	public static final class Placement
	{
		private final int index;
		private final int x;
		private final int y;
		private final int originalY;
		private final boolean sectionHead;

		Placement(int index, int x, int y, int originalY, boolean sectionHead)
		{
			this.index = index;
			this.x = x;
			this.y = y;
			this.originalY = originalY;
			this.sectionHead = sectionHead;
		}

		public int getIndex()
		{
			return index;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getOriginalY()
		{
			return originalY;
		}

		public boolean isSectionHead()
		{
			return sectionHead;
		}
	}

	public static final class PlacedLabel
	{
		private final int anchorIndex;
		private final String text;

		PlacedLabel(int anchorIndex, String text)
		{
			this.anchorIndex = anchorIndex;
			this.text = text;
		}

		public int getAnchorIndex()
		{
			return anchorIndex;
		}

		public String getText()
		{
			return text;
		}
	}

	public static final class Result
	{
		private static final Result EMPTY = new Result(0, Collections.emptyList(), Collections.emptyList());

		private final int headerHeight;
		private final List<Placement> placements;
		private final List<PlacedLabel> labels;

		private Result(int headerHeight, List<Placement> placements, List<PlacedLabel> labels)
		{
			this.headerHeight = headerHeight;
			this.placements = placements;
			this.labels = labels;
		}

		public static Result empty()
		{
			return EMPTY;
		}

		public boolean isEmpty()
		{
			return labels.isEmpty();
		}

		public List<Placement> getPlacements()
		{
			return placements;
		}

		public List<PlacedLabel> getLabels()
		{
			return labels;
		}

		public Placement placementFor(int index)
		{
			for (Placement placement : placements)
			{
				if (placement.getIndex() == index)
				{
					return placement;
				}
			}
			return null;
		}

		/**
		 * Y for a non-item widget (a tab separator on the all-items tab) that
		 * used to sit at {@code originalY}, so it stays above the items that
		 * were below it, including a section gap.
		 */
		public int yForNonItem(int originalY, int height)
		{
			int top = Integer.MAX_VALUE;
			boolean head = false;
			for (Placement placement : placements)
			{
				if (placement.getOriginalY() > originalY && placement.getY() < top)
				{
					top = placement.getY();
					head = placement.isSectionHead();
				}
			}
			if (top == Integer.MAX_VALUE)
			{
				return originalY;
			}
			int visualTop = head ? top - headerHeight : top;
			return Math.max(0, visualTop - height);
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

	public static Result plan(List<ItemSlot> items, List<SectionLabel> labels, int headerHeight, ItemIdentity identity, int containerWidth)
	{
		if (headerHeight <= 0 || items == null || items.isEmpty() || labels == null || labels.isEmpty())
		{
			return Result.empty();
		}

		List<ItemSlot> ordered = ordered(items);
		Grid grid = detectGrid(ordered, containerWidth);
		boolean[] heads = new boolean[ordered.size()];
		String[] texts = new String[ordered.size()];
		if (!markHeads(ordered, labels, identity, heads, texts))
		{
			return Result.empty();
		}

		int y = ordered.get(0).getY();
		int column = 0;
		int previousOriginalY = ordered.get(0).getY();
		List<Placement> placements = new ArrayList<>(ordered.size());
		List<PlacedLabel> placed = new ArrayList<>();

		for (int i = 0; i < ordered.size(); i++)
		{
			ItemSlot slot = ordered.get(i);
			if (slot.getY() > previousOriginalY)
			{
				int extra = slot.getY() - previousOriginalY - grid.rowStride;
				previousOriginalY = slot.getY();
				if (extra > 0)
				{
					if (column != 0)
					{
						column = 0;
						y += grid.rowStride;
					}
					y += extra;
				}
			}

			if (column >= grid.columns)
			{
				column = 0;
				y += grid.rowStride;
			}

			if (heads[i])
			{
				if (column != 0)
				{
					column = 0;
					y += grid.rowStride;
				}
				placed.add(new PlacedLabel(slot.getIndex(), texts[i]));
				y += headerHeight;
			}

			int x = grid.startX + column * grid.columnStride;
			placements.add(new Placement(slot.getIndex(), x, y, slot.getY(), heads[i]));
			column++;
		}

		return new Result(headerHeight, placements, placed);
	}

	private static boolean markHeads(List<ItemSlot> ordered, List<SectionLabel> labels, ItemIdentity identity, boolean[] heads, String[] texts)
	{
		Map<Integer, Integer> seen = new HashMap<>();
		boolean any = false;
		for (int i = 0; i < ordered.size(); i++)
		{
			int norm = normalize(ordered.get(i).getItemId(), identity);
			int occurrence = seen.getOrDefault(norm, 0);
			seen.put(norm, occurrence + 1);
			String text = null;
			for (SectionLabel label : labels)
			{
				if (label.getItemId() == norm && label.getOccurrence() == occurrence)
				{
					text = label.getText();
				}
			}
			if (text != null)
			{
				heads[i] = true;
				texts[i] = text;
				any = true;
			}
		}
		return any;
	}

	private static Grid detectGrid(List<ItemSlot> ordered, int containerWidth)
	{
		int startX = ordered.get(0).getX();
		for (ItemSlot slot : ordered)
		{
			if (slot.getX() < startX)
			{
				startX = slot.getX();
			}
		}

		int columnStride = COLUMN_STRIDE;
		TreeMap<Integer, Integer> rowCounts = new TreeMap<>();
		Map<Integer, Integer> strideCounts = new HashMap<>();
		int previousX = -1;
		int previousY = Integer.MIN_VALUE;
		for (ItemSlot slot : ordered)
		{
			rowCounts.put(slot.getY(), rowCounts.getOrDefault(slot.getY(), 0) + 1);
			if (slot.getY() == previousY && slot.getX() > previousX)
			{
				int diff = slot.getX() - previousX;
				if (diff >= 32 && diff <= 80)
				{
					strideCounts.put(diff, strideCounts.getOrDefault(diff, 0) + 1);
				}
			}
			previousX = slot.getX();
			previousY = slot.getY();
		}
		int bestStrideCount = 0;
		for (Map.Entry<Integer, Integer> entry : strideCounts.entrySet())
		{
			if (entry.getValue() > bestStrideCount)
			{
				bestStrideCount = entry.getValue();
				columnStride = entry.getKey();
			}
		}

		int columns = 8;
		if (containerWidth > startX && columnStride > 0)
		{
			int guess = (containerWidth - startX) / columnStride;
			if (guess >= 1 && guess <= 20)
			{
				columns = guess;
			}
		}
		if (rowCounts.size() >= 2)
		{
			columns = Math.max(1, rowCounts.firstEntry().getValue());
		}
		return new Grid(startX, columnStride, ROW_STRIDE, columns);
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

	private static final class Grid
	{
		private final int startX;
		private final int columnStride;
		private final int rowStride;
		private final int columns;

		private Grid(int startX, int columnStride, int rowStride, int columns)
		{
			this.startX = startX;
			this.columnStride = columnStride;
			this.rowStride = rowStride;
			this.columns = Math.max(1, columns);
		}
	}
}
