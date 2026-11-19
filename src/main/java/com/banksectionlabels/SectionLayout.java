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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Positional section grids for one bank tab.
 * <p>
 * A section is a row-major list of item ids. {@code -1} is an empty cell the
 * player can drop onto. The real bank order is not used to place those cells,
 * so a row can be helm, body, legs with gaps wherever the player left them.
 */
public final class SectionLayout
{
	public static final int EMPTY = -1;

	/** Space above the first heading so it is not glued to the top of the item box. */
	public static final int TOP_INSET = 8;

	/** Vertical band for a numbered-tab title on the all-items view. */
	public static final int TAB_HEADING_HEIGHT = 28;

	/** Space between one tab's last row and the next tab's title, so they do not overlap. */
	public static final int BLOCK_GAP = 12;

	private SectionLayout()
	{
	}

	public static final class Section
	{
		private final int tab;
		private final int columns;
		private final String name;
		private final List<Integer> cells;

		public Section(int tab, int columns, String name, List<Integer> cells)
		{
			this.tab = tab;
			this.columns = Math.max(1, columns);
			this.name = name == null ? "" : name;
			this.cells = new ArrayList<>();
			if (cells != null)
			{
				this.cells.addAll(cells);
			}
		}

		public int getTab()
		{
			return tab;
		}

		public int getColumns()
		{
			return columns;
		}

		public String getName()
		{
			return name;
		}

		public List<Integer> getCells()
		{
			return Collections.unmodifiableList(cells);
		}

		private Section copy()
		{
			return new Section(tab, columns, name, cells);
		}

		private void ensure(int cellIndex)
		{
			while (cells.size() <= cellIndex)
			{
				cells.add(EMPTY);
			}
		}
	}

	public static final class Spec
	{
		private final int columns;
		private final int headerHeight;
		private final int startX;
		private final int columnStride;
		private final int rowStride;

		public Spec(int columns, int headerHeight, int startX, int columnStride, int rowStride)
		{
			this.columns = Math.max(1, columns);
			this.headerHeight = Math.max(1, headerHeight);
			this.startX = startX;
			this.columnStride = columnStride;
			this.rowStride = rowStride;
		}

		public int getColumns()
		{
			return columns;
		}

		public int getHeaderHeight()
		{
			return headerHeight;
		}
	}

	public static final class Slot
	{
		private final int sectionIndex;
		private final int cellIndex;
		private final int itemId;
		private final int x;
		private final int y;
		private final boolean empty;

		Slot(int sectionIndex, int cellIndex, int itemId, int x, int y, boolean empty)
		{
			this.sectionIndex = sectionIndex;
			this.cellIndex = cellIndex;
			this.itemId = itemId;
			this.x = x;
			this.y = y;
			this.empty = empty;
		}

		public int getSectionIndex()
		{
			return sectionIndex;
		}

		public int getCellIndex()
		{
			return cellIndex;
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

		public boolean isEmpty()
		{
			return empty;
		}
	}

	public static final class Header
	{
		private final int tab;
		private final int sectionIndex;
		private final String name;
		private final int x;
		private final int y;
		private final int height;

		Header(int tab, int sectionIndex, String name, int x, int y, int height)
		{
			this.tab = tab;
			this.sectionIndex = sectionIndex;
			this.name = name;
			this.x = x;
			this.y = y;
			this.height = height;
		}

		public int getTab()
		{
			return tab;
		}

		public int getSectionIndex()
		{
			return sectionIndex;
		}

		public String getName()
		{
			return name;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getHeight()
		{
			return height;
		}
	}

	public static final class Arrangement
	{
		private final List<Slot> looseSlots;
		private final List<Slot> cells;
		private final List<Header> headers;
		private final int contentBottom;

		Arrangement(List<Slot> looseSlots, List<Slot> cells, List<Header> headers, int contentBottom)
		{
			this.looseSlots = looseSlots;
			this.cells = cells;
			this.headers = headers;
			this.contentBottom = contentBottom;
		}

		public List<Slot> getLooseSlots()
		{
			return looseSlots;
		}

		public List<Slot> getCells()
		{
			return cells;
		}

		public List<Header> getHeaders()
		{
			return headers;
		}

		public int getContentBottom()
		{
			return contentBottom;
		}
	}

	public static final class BankItem
	{
		private final int bankIndex;
		private final int itemId;

		public BankItem(int bankIndex, int itemId)
		{
			this.bankIndex = bankIndex;
			this.itemId = itemId;
		}

		public int getBankIndex()
		{
			return bankIndex;
		}

		public int getItemId()
		{
			return itemId;
		}
	}

	public static final class Assignment
	{
		private final int bankIndex;
		private final Slot slot;

		Assignment(int bankIndex, Slot slot)
		{
			this.bankIndex = bankIndex;
			this.slot = slot;
		}

		public int getBankIndex()
		{
			return bankIndex;
		}

		public Slot getSlot()
		{
			return slot;
		}
	}

	public static List<Section> addSection(List<Section> existing, int tab, String name, int columns, int itemId)
	{
		List<Section> copy = copyOf(existing);
		int cols = Math.max(1, columns);
		List<Integer> cells = new ArrayList<>();
		cells.add(itemId);
		for (int i = 1; i < cols; i++)
		{
			cells.add(EMPTY);
		}
		copy.add(new Section(tab, cols, SectionLabels.sanitize(name), cells));
		return copy;
	}

	public static List<Section> removeSection(List<Section> existing, int sectionIndex)
	{
		List<Section> copy = copyOf(existing);
		if (sectionIndex >= 0 && sectionIndex < copy.size())
		{
			copy.remove(sectionIndex);
		}
		return copy;
	}

	public static List<Section> rename(List<Section> existing, int sectionIndex, String name)
	{
		List<Section> copy = copyOf(existing);
		if (sectionIndex < 0 || sectionIndex >= copy.size())
		{
			return copy;
		}
		Section current = copy.get(sectionIndex);
		copy.set(sectionIndex, new Section(current.tab, current.columns, SectionLabels.sanitize(name), current.cells));
		return copy;
	}

	public static List<Section> clearCell(List<Section> existing, int sectionIndex, int cellIndex)
	{
		List<Section> copy = copyOf(existing);
		if (sectionIndex < 0 || sectionIndex >= copy.size())
		{
			return copy;
		}
		Section section = copy.get(sectionIndex);
		if (cellIndex < 0 || cellIndex >= section.cells.size())
		{
			return copy;
		}
		section.cells.set(cellIndex, EMPTY);
		return tidyAll(copy);
	}

	/**
	 * Move {@code sourceItemId} onto a section cell.
	 * Dropping on the section's first item from outside the section fills the
	 * first empty cell (the slot beside the head) instead of replacing the head.
	 * {@code sourceSection} is {@code -1} when the dragged item is not in a section.
	 */
	public static List<Section> drop(List<Section> existing, int sourceSection, int sourceCell, int sourceItemId, int targetSection, int targetCell)
	{
		List<Section> copy = copyOf(existing);
		if (targetSection < 0 || targetSection >= copy.size() || targetCell < 0 || sourceItemId < 0)
		{
			return copy;
		}
		if (sourceSection == targetSection && sourceCell == targetCell)
		{
			return copy;
		}

		Section target = copy.get(targetSection);
		target.ensure(targetCell);
		int destinationId = target.cells.get(targetCell);
		boolean ontoHeadFromOutside = targetCell == 0
			&& destinationId != EMPTY
			&& sourceSection != targetSection;

		if (ontoHeadFromOutside)
		{
			int empty = firstEmpty(target);
			if (empty < 0)
			{
				empty = growRow(target);
			}
			clearSource(copy, sourceSection, sourceCell);
			target.cells.set(empty, sourceItemId);
			return tidyAll(copy);
		}

		if (destinationId == EMPTY)
		{
			clearSource(copy, sourceSection, sourceCell);
			target.cells.set(targetCell, sourceItemId);
			return tidyAll(copy);
		}

		if (sourceSection >= 0 && sourceSection < copy.size() && sourceCell >= 0)
		{
			Section source = copy.get(sourceSection);
			if (sourceCell < source.cells.size())
			{
				int moving = source.cells.get(sourceCell);
				source.cells.set(sourceCell, destinationId);
				target.cells.set(targetCell, moving);
				return tidyAll(copy);
			}
		}

		target.cells.set(targetCell, sourceItemId);
		return tidyAll(copy);
	}

	public static Arrangement arrange(int looseCount, List<Section> sections, Spec spec)
	{
		return arrangeFrom(looseCount, sections, spec, TOP_INSET, false);
	}

	public static Arrangement arrangeFrom(int looseCount, List<Section> sections, Spec spec, int startY)
	{
		return arrangeFrom(looseCount, sections, spec, startY, false);
	}

	public static Arrangement arrangeFrom(int looseCount, List<Section> sections, Spec spec, int startY, boolean extraDropRow)
	{
		List<Slot> loose = new ArrayList<>();
		List<Slot> cells = new ArrayList<>();
		List<Header> headers = new ArrayList<>();
		int columns = spec.columns;
		int y = Math.max(0, startY);
		int looseIndex = 0;
		while (looseIndex < looseCount)
		{
			int column = looseIndex % columns;
			if (column == 0 && looseIndex > 0)
			{
				y += spec.rowStride;
			}
			loose.add(new Slot(-1, looseIndex, EMPTY, spec.startX + column * spec.columnStride, y, false));
			looseIndex++;
		}
		if (looseCount > 0)
		{
			y += spec.rowStride;
		}

		if (sections != null)
		{
			for (int s = 0; s < sections.size(); s++)
			{
				Section section = sections.get(s);
				int cols = section.columns > 0 ? section.columns : columns;
				if (spec.columns > 0 && spec.columns < cols)
				{
					cols = spec.columns;
				}
				int stored = section.cells.size();
				int occupiedRows = stored <= 0 ? 0 : (stored + cols - 1) / cols;
				// The spare row is a drop target only, and only while the caller asks for it.
				int rows = occupiedRows + (extraDropRow ? 1 : 0);
				int headerY = y;
				headers.add(new Header(section.tab, s, section.name, spec.startX, headerY, spec.headerHeight));
				int itemY = headerY + spec.headerHeight;
				for (int row = 0; row < rows; row++)
				{
					for (int column = 0; column < cols; column++)
					{
						int cellIndex = row * cols + column;
						int itemId = cellIndex < stored ? section.cells.get(cellIndex) : EMPTY;
						int cellY = itemY + row * spec.rowStride;
						cells.add(new Slot(s, cellIndex, itemId, spec.startX + column * spec.columnStride, cellY, itemId == EMPTY));
					}
				}
				y = itemY + rows * spec.rowStride;
			}
		}

		int bottom = y;
		if (!cells.isEmpty())
		{
			Slot last = cells.get(cells.size() - 1);
			bottom = Math.max(bottom, last.y + spec.rowStride);
		}
		return new Arrangement(loose, cells, headers, bottom);
	}

	public static String encode(List<Section> sections)
	{
		if (sections == null || sections.isEmpty())
		{
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (Section section : sections)
		{
			if (out.length() > 0)
			{
				out.append('\n');
			}
			out.append(section.tab)
				.append('\t')
				.append(section.columns)
				.append('\t')
				.append(section.name.replace('\t', ' '))
				.append('\t');
			for (int i = 0; i < section.cells.size(); i++)
			{
				if (i > 0)
				{
					out.append(',');
				}
				out.append(section.cells.get(i));
			}
		}
		return out.toString();
	}

	public static List<Section> decode(String raw)
	{
		List<Section> sections = new ArrayList<>();
		if (raw == null || raw.isEmpty())
		{
			return sections;
		}
		String[] lines = raw.split("\n", -1);
		for (String line : lines)
		{
			if (line.isEmpty())
			{
				continue;
			}
			String[] parts = line.split("\t", 4);
			if (parts.length < 4)
			{
				continue;
			}
			try
			{
				int tab = Integer.parseInt(parts[0].trim());
				if (isCellList(parts[3]))
				{
					int columns = Integer.parseInt(parts[1].trim());
					String name = SectionLabels.sanitize(parts[2]);
					if (tab < 0 || columns < 1 || name.isEmpty())
					{
						continue;
					}
					sections.add(new Section(tab, columns, name, parseCells(parts[3])));
				}
				else
				{
					// Older saves: tab, item id, occurrence, name.
					int itemId = Integer.parseInt(parts[1].trim());
					String name = SectionLabels.sanitize(parts[3]);
					if (tab < 0 || itemId < 0 || name.isEmpty())
					{
						continue;
					}
					List<Integer> cells = new ArrayList<>();
					cells.add(itemId);
					for (int i = 1; i < 8; i++)
					{
						cells.add(EMPTY);
					}
					sections.add(new Section(tab, 8, name, cells));
				}
			}
			catch (NumberFormatException ex)
			{
				// Skip one damaged line.
			}
		}
		return sections;
	}

	public static List<Section> forTab(List<Section> sections, int tab)
	{
		List<Section> matched = new ArrayList<>();
		if (sections == null)
		{
			return matched;
		}
		for (Section section : sections)
		{
			if (section.tab == tab)
			{
				matched.add(section.copy());
			}
		}
		return matched;
	}

	public static List<Section> replaceTab(List<Section> all, int tab, List<Section> tabSections)
	{
		List<Section> out = new ArrayList<>();
		if (all != null)
		{
			for (Section section : all)
			{
				if (section.tab != tab)
				{
					out.add(section.copy());
				}
			}
		}
		if (tabSections != null)
		{
			for (Section section : tabSections)
			{
				out.add(new Section(tab, section.columns, section.name, section.cells));
			}
		}
		return out;
	}

	private static int firstEmpty(Section section)
	{
		for (int i = 0; i < section.cells.size(); i++)
		{
			if (section.cells.get(i) == EMPTY)
			{
				return i;
			}
		}
		return -1;
	}

	private static int growRow(Section section)
	{
		int start = section.cells.size();
		for (int i = 0; i < section.columns; i++)
		{
			section.cells.add(EMPTY);
		}
		return start;
	}

	private static void clearSource(List<Section> sections, int sourceSection, int sourceCell)
	{
		if (sourceSection < 0 || sourceSection >= sections.size() || sourceCell < 0)
		{
			return;
		}
		Section source = sections.get(sourceSection);
		if (sourceCell < source.cells.size())
		{
			source.cells.set(sourceCell, EMPTY);
		}
	}

	private static List<Section> copyOf(List<Section> existing)
	{
		List<Section> copy = new ArrayList<>();
		if (existing != null)
		{
			for (Section section : existing)
			{
				copy.add(section.copy());
			}
		}
		return copy;
	}

	private static boolean isCellList(String value)
	{
		if (value == null || value.isEmpty())
		{
			return false;
		}
		String[] pieces = value.split(",", -1);
		for (String piece : pieces)
		{
			if (piece.isEmpty())
			{
				return false;
			}
			for (int i = 0; i < piece.length(); i++)
			{
				char c = piece.charAt(i);
				if (c == '-' && i == 0 && piece.length() > 1)
				{
					continue;
				}
				if (c < '0' || c > '9')
				{
					return false;
				}
			}
		}
		return true;
	}

	private static List<Integer> parseCells(String value)
	{
		List<Integer> cells = new ArrayList<>();
		if (value.isEmpty())
		{
			return cells;
		}
		for (String piece : value.split(",", -1))
		{
			cells.add(Integer.parseInt(piece.trim()));
		}
		return cells;
	}

	public static List<Integer> cells(int... ids)
	{
		List<Integer> cells = new ArrayList<>();
		for (int id : ids)
		{
			cells.add(id);
		}
		return cells;
	}

	public static Spec spec(int columns, int headerHeight)
	{
		return new Spec(columns, headerHeight, BankLayoutPlanner.START_X, BankLayoutPlanner.COLUMN_STRIDE, BankLayoutPlanner.ROW_STRIDE);
	}

	static List<Integer> copyCells(Section section)
	{
		return new ArrayList<>(section.cells);
	}

	public static boolean sameCells(Section section, Integer... expected)
	{
		return section.cells.equals(Arrays.asList(expected));
	}

	/**
	 * One drawn item or empty cell. {@code bankIndex} is the real bank slot, or -1 for an empty proxy.
	 * {@code sectionIndex} is the index inside that vanilla tab's section list.
	 */
	public static final class Placed
	{
		private final int bankIndex;
		private final int tab;
		private final int sectionIndex;
		private final int cellIndex;
		private final int x;
		private final int y;
		private final boolean empty;

		Placed(int bankIndex, int tab, int sectionIndex, int cellIndex, int x, int y, boolean empty)
		{
			this.bankIndex = bankIndex;
			this.tab = tab;
			this.sectionIndex = sectionIndex;
			this.cellIndex = cellIndex;
			this.x = x;
			this.y = y;
			this.empty = empty;
		}

		public int getBankIndex()
		{
			return bankIndex;
		}

		public int getTab()
		{
			return tab;
		}

		public int getSectionIndex()
		{
			return sectionIndex;
		}

		public int getCellIndex()
		{
			return cellIndex;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public boolean isEmpty()
		{
			return empty;
		}

		public boolean inSection()
		{
			return sectionIndex >= 0;
		}
	}

	/** Bold title drawn once above a numbered tab's block on the all-items view. */
	public static final class TabHeading
	{
		private final int tab;
		private final int x;
		private final int y;
		private final int height;

		TabHeading(int tab, int x, int y, int height)
		{
			this.tab = tab;
			this.x = x;
			this.y = y;
			this.height = height;
		}

		public int getTab()
		{
			return tab;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getHeight()
		{
			return height;
		}
	}

	/** Vertical span of one vanilla tab block, including its title. */
	public static final class TabSpan
	{
		private final int tab;
		private final int top;
		private final int bottom;

		TabSpan(int tab, int top, int bottom)
		{
			this.tab = tab;
			this.top = top;
			this.bottom = bottom;
		}

		public int getTab()
		{
			return tab;
		}

		public int getTop()
		{
			return top;
		}

		public int getBottom()
		{
			return bottom;
		}
	}

	public static final class BankView
	{
		private final List<Placed> placed;
		private final List<Header> headers;
		private final List<TabHeading> tabHeadings;
		private final List<TabSpan> spans;
		private final int contentBottom;
		private final List<Integer> separatorY;

		BankView(List<Placed> placed, List<Header> headers, List<TabHeading> tabHeadings, List<TabSpan> spans, int contentBottom, List<Integer> separatorY)
		{
			this.placed = placed;
			this.headers = headers;
			this.tabHeadings = tabHeadings;
			this.spans = spans == null ? Collections.emptyList() : spans;
			this.contentBottom = contentBottom;
			this.separatorY = separatorY;
		}

		public List<Placed> getPlaced()
		{
			return placed;
		}

		public List<Header> getHeaders()
		{
			return headers;
		}

		public List<TabHeading> getTabHeadings()
		{
			return tabHeadings;
		}

		public List<TabSpan> getSpans()
		{
			return spans;
		}

		public int getContentBottom()
		{
			return contentBottom;
		}

		public List<Integer> getSeparatorY()
		{
			return separatorY;
		}
	}

	/**
	 * Which vanilla tab owns a slot on the all-items view. {@code tabCounts[0]} is tab 1.
	 * Anything past the numbered tabs is untabbed (tab 0).
	 */
	public static int tabForSlot(int slot, int[] tabCounts)
	{
		if (slot < 0 || tabCounts == null)
		{
			return 0;
		}
		int start = 0;
		int limit = Math.min(9, tabCounts.length);
		for (int i = 0; i < limit; i++)
		{
			int count = Math.max(0, tabCounts[i]);
			if (slot < start + count)
			{
				return i + 1;
			}
			start += count;
		}
		return 0;
	}

	/**
	 * Layout for one open numbered tab. Every supplied item belongs to {@code tab}.
	 */
	public static BankView layoutTab(List<BankItem> items, int tab, List<Section> sections, Spec spec)
	{
		return layoutTab(items, tab, sections, spec, false);
	}

	public static BankView layoutTab(List<BankItem> items, int tab, List<Section> sections, Spec spec, boolean dragging)
	{
		List<BankItem> block = items == null ? new ArrayList<BankItem>() : new ArrayList<>(items);
		Collections.sort(block, Comparator.comparingInt(item -> item.bankIndex));
		BankView view = layoutBlock(block, tab, dedupe(sections), spec, TOP_INSET, false, dragging);
		List<TabSpan> spans = new ArrayList<>();
		spans.add(new TabSpan(tab, TOP_INSET, Math.max(TOP_INSET + 1, view.contentBottom)));
		return new BankView(view.placed, view.headers, view.tabHeadings, spans, view.contentBottom, view.separatorY);
	}

	/**
	 * All-items view, in the order the game draws it: untabbed items first, then tab 1,
	 * tab 2, and so on. Slot indexes are still tab 1, then tab 2, with the untabbed
	 * remainder at the end. Each numbered block gets a tab title, then that tab's
	 * loose items, then its sections. Tab 0 sections apply only to the untabbed items.
	 */
	public static BankView layoutAll(List<BankItem> items, int[] tabCounts, List<Section> sections, Spec spec)
	{
		return layoutAll(items, tabCounts, sections, spec, true, false, false);
	}

	public static BankView layoutAll(List<BankItem> items, int[] tabCounts, List<Section> sections, Spec spec, boolean tabTitles)
	{
		return layoutAll(items, tabCounts, sections, spec, tabTitles, false, false);
	}

	public static BankView layoutAll(List<BankItem> items, int[] tabCounts, List<Section> sections, Spec spec, boolean tabTitles, boolean showEmpty, boolean dragging)
	{
		List<Placed> placed = new ArrayList<>();
		List<Header> headers = new ArrayList<>();
		List<TabHeading> tabHeadings = new ArrayList<>();
		List<TabSpan> spans = new ArrayList<>();
		List<Integer> breaks = new ArrayList<>();
		int y = TOP_INSET;
		boolean any = false;
		int[] order = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		for (int tab : order)
		{
			List<BankItem> block = new ArrayList<>();
			if (items != null)
			{
				for (BankItem item : items)
				{
					if (tabForSlot(item.bankIndex, tabCounts) == tab)
					{
						block.add(item);
					}
				}
			}
			if (block.isEmpty())
			{
				continue;
			}
			Collections.sort(block, Comparator.comparingInt(item -> item.bankIndex));
			if (tab != 0)
			{
				if (any)
				{
					y += BLOCK_GAP;
				}
				if (tabTitles)
				{
					tabHeadings.add(new TabHeading(tab, spec.startX, y, TAB_HEADING_HEIGHT));
					breaks.add(y);
					y += TAB_HEADING_HEIGHT;
				}
				else if (any)
				{
					breaks.add(y - BLOCK_GAP / 2);
				}
			}
			any = true;
			int blockTop = tab != 0 && tabTitles ? y - TAB_HEADING_HEIGHT : y;
			BankView part = layoutBlock(block, tab, dedupe(forTab(sections, tab)), spec, y, !showEmpty, showEmpty && dragging);
			placed.addAll(part.placed);
			headers.addAll(part.headers);
			spans.add(new TabSpan(tab, blockTop, Math.max(blockTop + 1, part.contentBottom)));
			y = part.contentBottom;
		}
		return new BankView(placed, headers, tabHeadings, spans, y, breaks);
	}

	/**
	 * Container-content Y of a canvas point. Scroll is added because widget
	 * original Y is measured from the top of the scrolled content.
	 */
	public static int contentY(int mouseCanvasY, int containerCanvasY, int scrollY)
	{
		return mouseCanvasY - containerCanvasY + scrollY;
	}

	public static int contentX(int mouseCanvasX, int containerCanvasX)
	{
		return mouseCanvasX - containerCanvasX;
	}

	/**
	 * Which section cell contains a point in container-content coordinates.
	 * Returns {@code {tab, sectionIndex, cellIndex}}, or null.
	 */
	public static int[] hitTest(List<Placed> slots, int contentX, int contentY)
	{
		if (slots == null)
		{
			return null;
		}
		for (Placed slot : slots)
		{
			if (slot == null || !slot.inSection())
			{
				continue;
			}
			if (contentX >= slot.x && contentX < slot.x + BankLayoutPlanner.COLUMN_STRIDE
				&& contentY >= slot.y && contentY < slot.y + BankLayoutPlanner.ROW_STRIDE)
			{
				return new int[]{slot.tab, slot.sectionIndex, slot.cellIndex};
			}
		}
		return null;
	}

	/**
	 * Tab block under a point that missed every section cell. {@code -1} when
	 * the point is outside every block. Dropping there keeps the item unsectioned.
	 */
	public static int hitTab(List<TabSpan> spans, int contentX, int contentY)
	{
		if (spans == null || contentX < BankLayoutPlanner.START_X)
		{
			return -1;
		}
		for (TabSpan span : spans)
		{
			if (span != null && contentY >= span.top && contentY < span.bottom)
			{
				return span.tab;
			}
		}
		return -1;
	}

	/**
	 * A drop onto a different numbered tab has to move the real bank item into
	 * that tab. A drop inside the same tab only changes the saved grid.
	 */
	public static boolean movesVanillaTab(int sourceTab, int destTab)
	{
		return destTab > 0 && sourceTab != destTab;
	}

	/**
	 * Record a drop. When the source and destination tabs differ, the item is
	 * removed from the source tab's sections and written into the destination cell.
	 */
	public static List<Section> placeItem(List<Section> all, int sourceTab, int sourceSection, int sourceCell, int itemId, int destTab, int destSection, int destCell)
	{
		List<Section> next = all == null ? new ArrayList<Section>() : all;
		int fromSection = sourceSection;
		int fromCell = sourceCell;
		if (sourceTab != destTab)
		{
			if (sourceSection >= 0)
			{
				next = replaceTab(next, sourceTab, clearCell(forTab(next, sourceTab), sourceSection, sourceCell));
			}
			fromSection = -1;
			fromCell = -1;
		}
		List<Section> dest = drop(forTab(next, destTab), fromSection, fromCell, itemId, destSection, destCell);
		return replaceTab(next, destTab, dest);
	}

	private static BankView layoutBlock(List<BankItem> block, int tab, List<Section> sections, Spec spec, int startY, boolean packed, boolean dragging)
	{
		boolean[] used = new boolean[block.size()];
		List<int[]> cellMatches = new ArrayList<>();
		for (int s = 0; s < sections.size(); s++)
		{
			Section section = sections.get(s);
			int[] matches = new int[section.cells.size()];
			Arrays.fill(matches, -1);
			for (int c = 0; c < section.cells.size(); c++)
			{
				int id = section.cells.get(c);
				if (id < 0)
				{
					continue;
				}
				for (int i = 0; i < block.size(); i++)
				{
					if (!used[i] && block.get(i).itemId == id)
					{
						used[i] = true;
						matches[c] = i;
						break;
					}
				}
			}
			cellMatches.add(matches);
		}
		int looseCount = 0;
		for (boolean taken : used)
		{
			if (!taken)
			{
				looseCount++;
			}
		}
		Arrangement arranged = packed
			? arrangeFrom(looseCount, Collections.emptyList(), spec, startY, false)
			: arrangeFrom(looseCount, sections, spec, startY, dragging);
		List<Placed> placed = new ArrayList<>();
		int looseOrdinal = 0;
		int y = startY;
		for (int i = 0; i < block.size(); i++)
		{
			if (used[i])
			{
				continue;
			}
			if (looseOrdinal >= arranged.looseSlots.size())
			{
				break;
			}
			Slot slot = arranged.looseSlots.get(looseOrdinal++);
			placed.add(new Placed(block.get(i).bankIndex, tab, -1, -1, slot.x, slot.y, false));
			y = Math.max(y, slot.y + spec.rowStride);
		}
		if (looseCount > 0)
		{
			y = startY + ((looseCount + spec.columns - 1) / spec.columns) * spec.rowStride;
		}
		if (packed)
		{
			List<Header> headers = new ArrayList<>();
			if (sections != null)
			{
				for (int s = 0; s < sections.size(); s++)
				{
					Section section = sections.get(s);
					int cols = Math.max(1, section.columns);
					headers.add(new Header(section.tab, s, section.name, spec.startX, y, spec.headerHeight));
					int rowY = y + spec.headerHeight;
					int column = 0;
					boolean rowHasItem = false;
					int[] matches = s < cellMatches.size() ? cellMatches.get(s) : new int[0];
					for (int c = 0; c < section.cells.size(); c++)
					{
						if (c > 0 && c % cols == 0)
						{
							if (rowHasItem)
							{
								rowY += spec.rowStride;
							}
							column = 0;
							rowHasItem = false;
						}
						int id = section.cells.get(c);
						if (id < 0)
						{
							continue;
						}
						int item = c < matches.length ? matches[c] : -1;
						int x = spec.startX + column * spec.columnStride;
						if (item >= 0)
						{
							placed.add(new Placed(block.get(item).bankIndex, tab, s, c, x, rowY, false));
						}
						column++;
						rowHasItem = true;
					}
					if (rowHasItem)
					{
						rowY += spec.rowStride;
					}
					y = rowY;
				}
			}
			return new BankView(placed, headers, Collections.emptyList(), Collections.emptyList(), y, Collections.emptyList());
		}
		for (Slot slot : arranged.cells)
		{
			if (slot.empty && !keepHole(sections, slot, dragging))
			{
				continue;
			}
			if (slot.empty || slot.sectionIndex < 0 || slot.sectionIndex >= cellMatches.size())
			{
				placed.add(new Placed(-1, tab, slot.sectionIndex, slot.cellIndex, slot.x, slot.y, true));
				continue;
			}
			int[] matches = cellMatches.get(slot.sectionIndex);
			int item = slot.cellIndex >= 0 && slot.cellIndex < matches.length ? matches[slot.cellIndex] : -1;
			if (item < 0)
			{
				if (!keepHole(sections, slot, dragging))
				{
					continue;
				}
				placed.add(new Placed(-1, tab, slot.sectionIndex, slot.cellIndex, slot.x, slot.y, true));
			}
			else
			{
				placed.add(new Placed(block.get(item).bankIndex, tab, slot.sectionIndex, slot.cellIndex, slot.x, slot.y, false));
			}
		}
		return new BankView(placed, arranged.headers, Collections.emptyList(), Collections.emptyList(), arranged.contentBottom, Collections.emptyList());
	}

	/** Interior holes stay. Trailing holes and the spare row show only while dragging. */
	private static boolean keepHole(List<Section> sections, Slot slot, boolean dragging)
	{
		if (slot == null || !slot.empty)
		{
			return false;
		}
		if (dragging)
		{
			return true;
		}
		if (sections == null || slot.sectionIndex < 0 || slot.sectionIndex >= sections.size())
		{
			return false;
		}
		Section section = sections.get(slot.sectionIndex);
		int cols = Math.max(1, section.columns);
		if (slot.cellIndex >= section.cells.size())
		{
			return false;
		}
		int rowEnd = Math.min(section.cells.size(), (slot.cellIndex / cols) * cols + cols);
		for (int i = slot.cellIndex + 1; i < rowEnd; i++)
		{
			if (section.cells.get(i) >= 0)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Collapse sections in the same tab that share a name or the same head item.
	 * The copy with more filled cells is kept.
	 */
	public static List<Section> dedupe(List<Section> sections)
	{
		List<Section> out = new ArrayList<>();
		if (sections == null)
		{
			return out;
		}
		for (Section section : sections)
		{
			int found = -1;
			for (int i = 0; i < out.size(); i++)
			{
				if (out.get(i).tab == section.tab && (sameName(out.get(i), section) || sameHead(out.get(i), section)))
				{
					found = i;
					break;
				}
			}
			if (found < 0)
			{
				out.add(section.copy());
			}
			else
			{
				out.set(found, richer(out.get(found), section));
			}
		}
		return out;
	}

	/**
	 * Drop tab 0 copies that repeat a numbered tab's section name or head item.
	 * Those copies were written when the all-items view used its own sections.
	 */
	public static List<Section> dropTabZeroDuplicates(List<Section> sections)
	{
		List<Section> source = sections == null ? new ArrayList<Section>() : sections;
		Set<String> names = new HashSet<>();
		Set<Integer> heads = new HashSet<>();
		for (Section section : source)
		{
			if (section.tab <= 0)
			{
				continue;
			}
			names.add(normalizedName(section.name));
			int head = headId(section);
			if (head >= 0)
			{
				heads.add(head);
			}
		}
		List<Section> out = new ArrayList<>();
		for (Section section : source)
		{
			if (section.tab == 0
				&& (names.contains(normalizedName(section.name)) || heads.contains(headId(section))))
			{
				continue;
			}
			out.add(section.copy());
		}
		return out;
	}

	public static List<Section> prepare(List<Section> sections)
	{
		return tidyAll(dropTabZeroDuplicates(dedupe(sections)));
	}

	/**
	 * Drop fully empty rows, drop trailing holes on each row, and shrink the
	 * width to the rightmost item. Holes between two items on the same row stay.
	 */
	public static Section tidy(Section section)
	{
		if (section == null)
		{
			return null;
		}
		int cols = Math.max(1, section.columns);
		List<List<Integer>> rows = new ArrayList<>();
		for (int i = 0; i < section.cells.size(); i += cols)
		{
			List<Integer> row = new ArrayList<>();
			for (int c = 0; c < cols && i + c < section.cells.size(); c++)
			{
				row.add(section.cells.get(i + c));
			}
			boolean any = false;
			for (int id : row)
			{
				if (id >= 0)
				{
					any = true;
					break;
				}
			}
			if (any)
			{
				rows.add(row);
			}
		}
		int width = 1;
		boolean anyItem = false;
		for (List<Integer> row : rows)
		{
			for (int c = 0; c < row.size(); c++)
			{
				if (row.get(c) >= 0)
				{
					anyItem = true;
					width = Math.max(width, c + 1);
				}
			}
		}
		if (!anyItem)
		{
			return new Section(section.tab, 1, section.name, new ArrayList<Integer>());
		}
		List<Integer> cells = new ArrayList<>();
		for (List<Integer> row : rows)
		{
			for (int c = 0; c < width; c++)
			{
				cells.add(c < row.size() && row.get(c) != null ? row.get(c) : EMPTY);
			}
		}
		while (!cells.isEmpty() && cells.get(cells.size() - 1) == EMPTY)
		{
			cells.remove(cells.size() - 1);
		}
		return new Section(section.tab, width, section.name, cells);
	}

	/**
	 * Remove every hole, including gaps between items, but keep each occupied
	 * row on its own line.
	 */
	public static Section compact(Section section)
	{
		if (section == null)
		{
			return null;
		}
		int cols = Math.max(1, section.columns);
		List<List<Integer>> rows = new ArrayList<>();
		int width = 1;
		for (int i = 0; i < section.cells.size(); i += cols)
		{
			List<Integer> row = new ArrayList<>();
			for (int c = 0; c < cols && i + c < section.cells.size(); c++)
			{
				int id = section.cells.get(i + c);
				if (id >= 0)
				{
					row.add(id);
				}
			}
			if (!row.isEmpty())
			{
				width = Math.max(width, row.size());
				rows.add(row);
			}
		}
		List<Integer> cells = new ArrayList<>();
		for (List<Integer> row : rows)
		{
			cells.addAll(row);
			for (int c = row.size(); c < width; c++)
			{
				cells.add(EMPTY);
			}
		}
		return tidy(new Section(section.tab, Math.max(1, width), section.name, cells));
	}

	public static List<Section> compactSection(List<Section> sections, int sectionIndex)
	{
		List<Section> copy = copyOf(sections);
		if (sectionIndex < 0 || sectionIndex >= copy.size())
		{
			return copy;
		}
		copy.set(sectionIndex, compact(copy.get(sectionIndex)));
		return copy;
	}

	/**
	 * Take an item out of whatever section held it. It stays in the tab's
	 * unsectioned area and is not written into another section.
	 */
	public static List<Section> releaseToUnsectioned(List<Section> all, int sourceTab, int sourceSection, int sourceCell)
	{
		List<Section> next = all == null ? new ArrayList<Section>() : all;
		if (sourceSection < 0)
		{
			return tidyAll(next);
		}
		return replaceTab(next, sourceTab, clearCell(forTab(next, sourceTab), sourceSection, sourceCell));
	}

	private static List<Section> tidyAll(List<Section> sections)
	{
		List<Section> out = new ArrayList<>();
		if (sections == null)
		{
			return out;
		}
		for (Section section : sections)
		{
			Section tidied = tidy(section);
			if (tidied != null)
			{
				out.add(tidied);
			}
		}
		return out;
	}

	/**
	 * Add or edit a heading. An item that already heads a section is renamed.
	 * A name that already exists absorbs the item instead of creating a second section.
	 * An empty name removes the section that item heads.
	 */
	public static List<Section> labelItem(List<Section> tabSections, int tab, String name, int columns, int itemId)
	{
		List<Section> sections = copyOf(tabSections);
		String clean = SectionLabels.sanitize(name);
		int byHead = indexWithHead(sections, itemId);
		int byName = indexWithName(sections, clean);
		if (clean.isEmpty())
		{
			if (byHead >= 0)
			{
				return removeSection(sections, byHead);
			}
			return sections;
		}
		if (byHead >= 0)
		{
			if (byName >= 0 && byName != byHead)
			{
				sections = removeSection(sections, byHead);
				int target = indexWithName(sections, clean);
				if (containsItem(sections.get(target), itemId))
				{
					return sections;
				}
				return drop(sections, -1, -1, itemId, target, firstEmptyOrGrow(sections.get(target)));
			}
			return rename(sections, byHead, clean);
		}
		sections = stripItem(sections, itemId);
		byName = indexWithName(sections, clean);
		if (byName >= 0)
		{
			if (containsItem(sections.get(byName), itemId))
			{
				return sections;
			}
			return drop(sections, -1, -1, itemId, byName, firstEmptyOrGrow(sections.get(byName)));
		}
		return addSection(sections, tab, clean, columns, itemId);
	}

	private static List<Section> stripItem(List<Section> sections, int itemId)
	{
		if (itemId < 0)
		{
			return sections;
		}
		for (Section section : sections)
		{
			for (int i = 0; i < section.cells.size(); i++)
			{
				if (section.cells.get(i) == itemId)
				{
					section.cells.set(i, EMPTY);
				}
			}
		}
		return sections;
	}

	private static int firstEmptyOrGrow(Section section)
	{
		int empty = firstEmpty(section);
		return empty >= 0 ? empty : section.cells.size();
	}

	private static boolean containsItem(Section section, int itemId)
	{
		for (int id : section.cells)
		{
			if (id == itemId)
			{
				return true;
			}
		}
		return false;
	}

	private static int indexWithHead(List<Section> sections, int itemId)
	{
		if (itemId < 0)
		{
			return -1;
		}
		for (int i = 0; i < sections.size(); i++)
		{
			if (headId(sections.get(i)) == itemId)
			{
				return i;
			}
		}
		return -1;
	}

	private static int indexWithName(List<Section> sections, String name)
	{
		if (name == null || name.isEmpty())
		{
			return -1;
		}
		String key = normalizedName(name);
		for (int i = 0; i < sections.size(); i++)
		{
			if (normalizedName(sections.get(i).name).equals(key))
			{
				return i;
			}
		}
		return -1;
	}

	private static boolean sameName(Section left, Section right)
	{
		return normalizedName(left.name).equals(normalizedName(right.name));
	}

	private static boolean sameHead(Section left, Section right)
	{
		int a = headId(left);
		int b = headId(right);
		return a >= 0 && a == b;
	}

	private static int headId(Section section)
	{
		if (section.cells.isEmpty())
		{
			return -1;
		}
		int id = section.cells.get(0);
		return id < 0 ? -1 : id;
	}

	private static String normalizedName(String name)
	{
		return name == null ? "" : name.trim().toLowerCase(Locale.ENGLISH);
	}

	private static Section richer(Section left, Section right)
	{
		int leftFilled = filled(left);
		int rightFilled = filled(right);
		if (rightFilled > leftFilled)
		{
			return right.copy();
		}
		if (leftFilled > rightFilled)
		{
			return left.copy();
		}
		if (headId(left) < 0 && headId(right) >= 0)
		{
			return right.copy();
		}
		return left.copy();
	}

	private static int filled(Section section)
	{
		int n = 0;
		for (int id : section.cells)
		{
			if (id >= 0)
			{
				n++;
			}
		}
		return n;
	}
}
