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
		assertEquals(0, arranged.getLooseSlots().get(0).getY());
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
		assertEquals(20, firstY);
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
}
