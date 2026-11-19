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
import java.util.List;

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
		private final int sectionIndex;
		private final String name;
		private final int x;
		private final int y;
		private final int height;

		Header(int sectionIndex, String name, int x, int y, int height)
		{
			this.sectionIndex = sectionIndex;
			this.name = name;
			this.x = x;
			this.y = y;
			this.height = height;
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
		return copy;
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
			return copy;
		}

		if (destinationId == EMPTY)
		{
			clearSource(copy, sourceSection, sourceCell);
			target.cells.set(targetCell, sourceItemId);
			return copy;
		}

		if (sourceSection >= 0 && sourceSection < copy.size() && sourceCell >= 0)
		{
			Section source = copy.get(sourceSection);
			if (sourceCell < source.cells.size())
			{
				int moving = source.cells.get(sourceCell);
				source.cells.set(sourceCell, destinationId);
				target.cells.set(targetCell, moving);
				return copy;
			}
		}

		target.cells.set(targetCell, sourceItemId);
		return copy;
	}

	public static Arrangement arrange(int looseCount, List<Section> sections, Spec spec)
	{
		List<Slot> loose = new ArrayList<>();
		List<Slot> cells = new ArrayList<>();
		List<Header> headers = new ArrayList<>();
		int columns = spec.columns;
		int y = 0;
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
				int occupiedRows = Math.max(1, (Math.max(1, stored) + cols - 1) / cols);
				boolean lastFull = stored > 0 && stored % cols == 0 && lastRowOccupied(section, cols);
				int rows = occupiedRows + (lastFull ? 1 : 0);
				int headerY = y;
				headers.add(new Header(s, section.name, spec.startX, headerY, spec.headerHeight));
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

	private static boolean lastRowOccupied(Section section, int columns)
	{
		int start = section.cells.size() - columns;
		if (start < 0)
		{
			return false;
		}
		for (int i = start; i < section.cells.size(); i++)
		{
			if (section.cells.get(i) == EMPTY)
			{
				return false;
			}
		}
		return true;
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
}
