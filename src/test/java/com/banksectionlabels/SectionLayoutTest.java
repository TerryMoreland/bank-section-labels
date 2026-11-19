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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SectionLayoutTest
{
	private static final int HELM = 4716;
	private static final int BODY = 4724;
	private static final int LEGS = 4732;
	private static final int TINDERBOX = 590;
	private static final int LOGS = 1511;

	@Test
	public void armorSetKeepsEmptyCellsInOneRow()
	{
		SectionLayout.Section set = new SectionLayout.Section(6, 8, "Dharok",
			SectionLayout.cells(HELM, BODY, SectionLayout.EMPTY, LEGS));
		SectionLayout.Arrangement arranged = SectionLayout.arrange(0, Collections.singletonList(set), SectionLayout.spec(8, 24));

		SectionLayout.Slot helm = cell(arranged, 0);
		SectionLayout.Slot body = cell(arranged, 1);
		SectionLayout.Slot gap = cell(arranged, 2);
		SectionLayout.Slot legs = cell(arranged, 3);

		assertEquals(BankLayoutPlanner.START_X, helm.getX());
		assertEquals(BankLayoutPlanner.START_X + BankLayoutPlanner.COLUMN_STRIDE, body.getX());
		assertEquals(BankLayoutPlanner.START_X + 2 * BankLayoutPlanner.COLUMN_STRIDE, gap.getX());
		assertEquals(BankLayoutPlanner.START_X + 3 * BankLayoutPlanner.COLUMN_STRIDE, legs.getX());
		assertEquals(helm.getY(), body.getY());
		assertEquals(helm.getY(), gap.getY());
		assertEquals(helm.getY(), legs.getY());
		assertTrue(gap.isEmpty());
		assertEquals(HELM, helm.getItemId());
		assertEquals(LEGS, legs.getItemId());
	}

	@Test
	public void headerSitsStrictlyAboveTheSectionItems()
	{
		SectionLayout.Section section = new SectionLayout.Section(0, 8, "Mining", SectionLayout.cells(436, SectionLayout.EMPTY));
		SectionLayout.Arrangement arranged = SectionLayout.arrange(3, Collections.singletonList(section), SectionLayout.spec(8, 24));

		SectionLayout.Header header = arranged.getHeaders().get(0);
		SectionLayout.Slot first = cell(arranged, 0);
		assertEquals(header.getY() + header.getHeight(), first.getY());
		assertTrue(first.getY() >= header.getY() + 24);
		assertEquals(SectionLayout.TOP_INSET, arranged.getLooseSlots().get(0).getY());
		assertTrue(header.getY() >= arranged.getLooseSlots().get(2).getY() + BankLayoutPlanner.ROW_STRIDE);
	}

	@Test
	public void secondSectionStartsBelowTheFirst()
	{
		List<SectionLayout.Section> sections = Arrays.asList(
			new SectionLayout.Section(0, 8, "Woodcutting", SectionLayout.cells(TINDERBOX, LOGS)),
			new SectionLayout.Section(0, 8, "Mining", SectionLayout.cells(436, 1265))
		);
		SectionLayout.Arrangement arranged = SectionLayout.arrange(0, sections, SectionLayout.spec(8, 20));

		int firstY = cell(arranged, 0, 0).getY();
		int secondY = cell(arranged, 1, 0).getY();
		assertEquals(SectionLayout.TOP_INSET + 20, firstY);
		assertTrue(secondY >= firstY + BankLayoutPlanner.ROW_STRIDE + 20);
		assertEquals("Woodcutting", arranged.getHeaders().get(0).getName());
		assertEquals("Mining", arranged.getHeaders().get(1).getName());
		assertEquals(arranged.getHeaders().get(1).getY() + 20, secondY);
	}

	@Test
	public void dropBesideTheHeadFillsTheNextEmptyCell()
	{
		List<SectionLayout.Section> sections = SectionLayout.addSection(Collections.emptyList(), 0, "Woodcutting and Firemaking", 8, TINDERBOX);

		List<SectionLayout.Section> droppedOnGap = SectionLayout.drop(sections, -1, -1, LOGS, 0, 1);
		assertEquals(Integer.valueOf(TINDERBOX), droppedOnGap.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(LOGS), droppedOnGap.get(0).getCells().get(1));

		List<SectionLayout.Section> droppedOnHead = SectionLayout.drop(sections, -1, -1, LOGS, 0, 0);
		assertEquals(Integer.valueOf(TINDERBOX), droppedOnHead.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(LOGS), droppedOnHead.get(0).getCells().get(1));
	}

	@Test
	public void addingASecondSectionKeepsTheFirst()
	{
		List<SectionLayout.Section> one = SectionLayout.addSection(Collections.emptyList(), 0, "Woodcutting and Firemaking", 8, TINDERBOX);
		List<SectionLayout.Section> two = SectionLayout.addSection(one, 0, "Mining", 8, 1265);

		assertEquals(2, two.size());
		assertEquals("Woodcutting and Firemaking", two.get(0).getName());
		assertEquals("Mining", two.get(1).getName());
		assertEquals(Integer.valueOf(TINDERBOX), two.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(1265), two.get(1).getCells().get(0));
	}

	@Test
	public void swapAndMoveWithinASection()
	{
		List<SectionLayout.Section> sections = Collections.singletonList(
			new SectionLayout.Section(6, 8, "Dharok", SectionLayout.cells(HELM, BODY, SectionLayout.EMPTY, LEGS)));

		List<SectionLayout.Section> moved = SectionLayout.drop(sections, 0, 3, LEGS, 0, 2);
		assertEquals(Integer.valueOf(EMPTY()), moved.get(0).getCells().get(3));
		assertEquals(Integer.valueOf(LEGS), moved.get(0).getCells().get(2));

		List<SectionLayout.Section> swapped = SectionLayout.drop(sections, 0, 0, HELM, 0, 1);
		assertEquals(Integer.valueOf(BODY), swapped.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(HELM), swapped.get(0).getCells().get(1));
	}

	@Test
	public void oldSingleLabelSaveBecomesARowWithEmptySlots()
	{
		List<SectionLayout.Section> sections = SectionLayout.decode("0\t590\t0\tWoodcutting and Firemaking");

		assertEquals(1, sections.size());
		assertEquals(0, sections.get(0).getTab());
		assertEquals("Woodcutting and Firemaking", sections.get(0).getName());
		assertEquals(Integer.valueOf(590), sections.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(SectionLayout.EMPTY), sections.get(0).getCells().get(1));
		assertEquals(8, sections.get(0).getCells().size());
	}

	@Test
	public void roundTripPreservesEmptyCells()
	{
		List<SectionLayout.Section> sections = Collections.singletonList(
			new SectionLayout.Section(0, 8, "Barrows", SectionLayout.cells(HELM, BODY, SectionLayout.EMPTY, LEGS)));

		List<SectionLayout.Section> decoded = SectionLayout.decode(SectionLayout.encode(sections));

		assertEquals(1, decoded.size());
		assertEquals("Barrows", decoded.get(0).getName());
		assertEquals(sections.get(0).getCells(), decoded.get(0).getCells());
	}

	@Test
	public void labelingEditsTheSameSectionAndSurvivesReopen()
	{
		List<SectionLayout.Section> sections = SectionLayout.labelItem(Collections.emptyList(), 1, "Woodcutting", 8, TINDERBOX);
		sections = SectionLayout.drop(sections, -1, -1, 1521, 0, 1);
		sections = SectionLayout.drop(sections, -1, -1, LOGS, 0, 0);
		sections = SectionLayout.decode(SectionLayout.encode(sections));

		assertEquals(1, sections.size());
		assertEquals(Integer.valueOf(TINDERBOX), sections.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(1521), sections.get(0).getCells().get(1));
		assertEquals(Integer.valueOf(LOGS), sections.get(0).getCells().get(2));

		sections = SectionLayout.labelItem(sections, 1, "Mining", 8, 1265);
		assertEquals(2, sections.size());

		sections = SectionLayout.labelItem(sections, 1, "Rocks", 8, 1265);
		assertEquals(2, sections.size());
		assertEquals("Rocks", sections.get(1).getName());
		assertEquals(Integer.valueOf(1265), sections.get(1).getCells().get(0));

		sections = SectionLayout.labelItem(sections, 1, "Rocks", 8, 999);
		assertEquals(2, sections.size());
		assertEquals(Integer.valueOf(999), sections.get(1).getCells().get(1));
	}

	@Test
	public void duplicateNameOrHeadCollapsesToOneSection()
	{
		List<SectionLayout.Section> sections = new ArrayList<>();
		sections.add(new SectionLayout.Section(1, 8, "Mining", SectionLayout.cells(1265, SectionLayout.EMPTY)));
		sections.add(new SectionLayout.Section(1, 8, "Mining", SectionLayout.cells(SectionLayout.EMPTY)));
		sections.add(new SectionLayout.Section(0, 8, "Woodcutting", SectionLayout.cells(TINDERBOX)));
		sections.add(new SectionLayout.Section(1, 8, "Woodcutting", SectionLayout.cells(TINDERBOX, LOGS)));

		List<SectionLayout.Section> prepared = SectionLayout.prepare(sections);

		assertEquals(2, prepared.size());
		assertEquals(1, prepared.get(0).getTab());
		assertEquals("Mining", prepared.get(0).getName());
		assertEquals(Integer.valueOf(1265), prepared.get(0).getCells().get(0));
		assertEquals("Woodcutting", prepared.get(1).getName());
		assertEquals(Integer.valueOf(LOGS), prepared.get(1).getCells().get(1));
	}

	@Test
	public void mainViewShowsEachTabUnderItsOwnTitle()
	{
		int oak = 1521;
		int pick = 1265;
		int coins = 995;
		int bow = 841;
		int arrow = 882;
		int net = 303;
		int[] counts = {4, 2, 1, 0, 0, 0, 0, 0, 0};
		List<SectionLayout.BankItem> items = Arrays.asList(
			new SectionLayout.BankItem(0, TINDERBOX),
			new SectionLayout.BankItem(1, oak),
			new SectionLayout.BankItem(2, LOGS),
			new SectionLayout.BankItem(3, pick),
			new SectionLayout.BankItem(4, bow),
			new SectionLayout.BankItem(5, arrow),
			new SectionLayout.BankItem(6, net),
			new SectionLayout.BankItem(7, coins)
		);
		List<SectionLayout.Section> sections = new ArrayList<>();
		sections.add(new SectionLayout.Section(0, 8, "Woodcutting", SectionLayout.cells(TINDERBOX)));
		sections.add(new SectionLayout.Section(1, 8, "Woodcutting", SectionLayout.cells(TINDERBOX, oak, LOGS, SectionLayout.EMPTY)));
		sections.add(new SectionLayout.Section(1, 8, "Mining", SectionLayout.cells(pick, SectionLayout.EMPTY)));
		sections.add(new SectionLayout.Section(2, 8, "Bows", SectionLayout.cells(bow, arrow)));

		SectionLayout.BankView view = SectionLayout.layoutAll(items, counts, SectionLayout.prepare(sections), SectionLayout.spec(8, 24));

		SectionLayout.Placed coin = placed(view, 7);
		SectionLayout.Placed tinder = placed(view, 0);
		SectionLayout.Placed oakItem = placed(view, 1);
		SectionLayout.Placed logs = placed(view, 2);
		SectionLayout.Placed pickaxe = placed(view, 3);
		SectionLayout.Placed bowItem = placed(view, 4);
		SectionLayout.Placed netItem = placed(view, 6);

		assertTrue(coin.getY() < tinder.getY());
		assertEquals(-1, coin.getSectionIndex());
		assertEquals(tinder.getY(), oakItem.getY());
		assertEquals(tinder.getY(), logs.getY());
		assertTrue(pickaxe.getY() > tinder.getY());
		assertEquals(1, pickaxe.getTab());
		assertEquals(1, pickaxe.getSectionIndex());
		assertTrue(bowItem.getY() > pickaxe.getY());
		assertEquals(2, bowItem.getTab());
		assertTrue(netItem.getY() > bowItem.getY());
		assertEquals(-1, netItem.getSectionIndex());

		assertEquals(3, view.getTabHeadings().size());
		assertEquals(1, view.getTabHeadings().get(0).getTab());
		assertEquals(2, view.getTabHeadings().get(1).getTab());
		assertEquals(3, view.getTabHeadings().get(2).getTab());
		assertTrue(view.getTabHeadings().get(0).getY() + view.getTabHeadings().get(0).getHeight() <= headerNamed(view, "Woodcutting").getY());
		assertTrue(headerNamed(view, "Woodcutting").getY() < tinder.getY());
		assertTrue(headerNamed(view, "Mining").getY() < pickaxe.getY());
		assertTrue(headerNamed(view, "Bows").getY() < bowItem.getY());
		assertTrue(view.getTabHeadings().get(0).getY() > coin.getY());
		assertEquals(BankLayoutPlanner.START_X, view.getTabHeadings().get(0).getX());
	}

	@Test
	public void cellBelowAnItemIsARealDropTarget()
	{
		List<SectionLayout.Section> sections = Collections.singletonList(
			new SectionLayout.Section(2, 8, "Barrows", SectionLayout.cells(HELM, BODY, LEGS, 4587)));
		SectionLayout.Arrangement arranged = SectionLayout.arrange(0, sections, SectionLayout.spec(8, 24));

		SectionLayout.Slot helm = cell(arranged, 0);
		SectionLayout.Slot below = cell(arranged, 8);
		assertEquals(helm.getX(), below.getX());
		assertEquals(helm.getY() + BankLayoutPlanner.ROW_STRIDE, below.getY());
		assertTrue(below.isEmpty());

		List<SectionLayout.Section> dropped = SectionLayout.drop(sections, -1, -1, 11840, 0, 8);
		assertEquals(Integer.valueOf(HELM), dropped.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(11840), dropped.get(0).getCells().get(8));
	}

	@Test
	public void mainViewHitTestUsesHeaderAndScrollCoordinates()
	{
		int[] counts = {1, 0, 0, 0, 0, 0, 0, 0, 0};
		List<SectionLayout.BankItem> items = Arrays.asList(
			new SectionLayout.BankItem(0, TINDERBOX),
			new SectionLayout.BankItem(1, 995)
		);
		List<SectionLayout.Section> sections = Collections.singletonList(
			new SectionLayout.Section(1, 8, "Firemaking", SectionLayout.cells(TINDERBOX)));
		SectionLayout.BankView view = SectionLayout.layoutAll(items, counts, sections, SectionLayout.spec(8, 24));

		SectionLayout.Placed below = null;
		for (SectionLayout.Placed placed : view.getPlaced())
		{
			if (placed.getTab() == 1 && placed.getCellIndex() == 8)
			{
				below = placed;
				break;
			}
		}
		assertTrue(below != null);
		assertTrue(below.getY() > view.getTabHeadings().get(0).getY() + view.getTabHeadings().get(0).getHeight());

		int[] hit = SectionLayout.hitTest(view.getPlaced(), below.getX() + 3, below.getY() + 3);
		assertEquals(1, hit[0]);
		assertEquals(0, hit[1]);
		assertEquals(8, hit[2]);

		int originY = 400;
		int scroll = 90;
		int mouseY = originY + below.getY() - scroll + 2;
		int contentY = SectionLayout.contentY(mouseY, originY, scroll);
		int[] scrolled = SectionLayout.hitTest(view.getPlaced(), SectionLayout.contentX(below.getX() + 1, 0), contentY);
		assertEquals(8, scrolled[2]);

		int headingY = view.getTabHeadings().get(0).getY() + 4;
		assertTrue(SectionLayout.hitTest(view.getPlaced(), below.getX() + 1, headingY) == null);
	}

	@Test
	public void dropIntoAnotherTabsSectionRecordsThatCell()
	{
		assertTrue(SectionLayout.movesVanillaTab(0, 2));
		assertTrue(!SectionLayout.movesVanillaTab(2, 2));
		assertTrue(!SectionLayout.movesVanillaTab(1, 0));

		List<SectionLayout.Section> sections = Collections.singletonList(
			new SectionLayout.Section(2, 8, "Bows", SectionLayout.cells(841)));
		List<SectionLayout.Section> placed = SectionLayout.placeItem(sections, 0, -1, -1, 882, 2, 0, 8);
		assertEquals(1, placed.size());
		assertEquals(2, placed.get(0).getTab());
		assertEquals(Integer.valueOf(841), placed.get(0).getCells().get(0));
		assertEquals(Integer.valueOf(882), placed.get(0).getCells().get(8));
	}

	@Test
	public void hiddenTabTitlesLeaveTheItemOrderIntact()
	{
		int[] counts = {1, 0, 0, 0, 0, 0, 0, 0, 0};
		List<SectionLayout.BankItem> items = Arrays.asList(
			new SectionLayout.BankItem(0, TINDERBOX),
			new SectionLayout.BankItem(1, 995)
		);
		SectionLayout.BankView view = SectionLayout.layoutAll(items, counts, Collections.emptyList(), SectionLayout.spec(8, 24), false);
		assertTrue(view.getTabHeadings().isEmpty());
		assertTrue(placed(view, 1).getY() < placed(view, 0).getY());
	}

	private static SectionLayout.Slot cell(SectionLayout.Arrangement arranged, int cellIndex)
	{
		return cell(arranged, 0, cellIndex);
	}

	private static SectionLayout.Slot cell(SectionLayout.Arrangement arranged, int sectionIndex, int cellIndex)
	{
		for (SectionLayout.Slot slot : arranged.getCells())
		{
			if (slot.getSectionIndex() == sectionIndex && slot.getCellIndex() == cellIndex)
			{
				return slot;
			}
		}
		throw new AssertionError("missing cell " + sectionIndex + ":" + cellIndex);
	}

	private static int EMPTY()
	{
		return SectionLayout.EMPTY;
	}

	private static SectionLayout.Placed placed(SectionLayout.BankView view, int bankIndex)
	{
		for (SectionLayout.Placed placed : view.getPlaced())
		{
			if (placed.getBankIndex() == bankIndex)
			{
				return placed;
			}
		}
		throw new AssertionError("missing bank index " + bankIndex);
	}

	private static SectionLayout.Header headerNamed(SectionLayout.BankView view, String name)
	{
		for (SectionLayout.Header header : view.getHeaders())
		{
			if (name.equals(header.getName()))
			{
				return header;
			}
		}
		throw new AssertionError("missing header " + name);
	}
}
