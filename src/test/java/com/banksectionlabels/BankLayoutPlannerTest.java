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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BankLayoutPlannerTest
{
	private static final int REAL_ID = 249;
	private static final int PLACEHOLDER_ID = 2249;

	private final BankLayoutPlanner.ItemIdentity identity = new BankLayoutPlanner.ItemIdentity()
	{
		@Override
		public boolean isPlaceholder(int itemId)
		{
			return itemId == PLACEHOLDER_ID;
		}

		@Override
		public int linkedId(int itemId)
		{
			if (itemId == PLACEHOLDER_ID)
			{
				return REAL_ID;
			}
			if (itemId == REAL_ID)
			{
				return PLACEHOLDER_ID;
			}
			return -1;
		}
	};

	@Test
	public void labeledRowAndEverythingBelowItMovesDown()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(0, 100, 51, 0),
			slot(1, 200, 51, 36),
			slot(2, 300, 51, 72)
		);
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, 200, 0, "Herbs"));

		BankLayoutPlanner.Result plan = BankLayoutPlanner.plan(items, labels, 16, identity);

		assertEquals(0, plan.shiftForY(0));
		assertEquals(16, plan.shiftForY(36));
		assertEquals(16, plan.shiftForY(72));
		// A tab separator that sits just above the labeled row stays put.
		assertEquals(0, plan.shiftForY(32));
		// A separator under the labeled row moves with the rows below it.
		assertEquals(16, plan.shiftForY(70));
		assertEquals(16, plan.getExtraScroll());
		assertEquals(1, plan.getLabels().size());
		assertEquals("Herbs", plan.getLabels().get(0).getText());
		assertEquals(1, plan.getLabels().get(0).getAnchorIndex());
	}

	@Test
	public void gapsStackWhenSeveralRowsAreLabeled()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(0, 10, 51, 0),
			slot(1, 20, 51, 36),
			slot(2, 30, 51, 80)
		);
		List<SectionLabel> labels = Arrays.asList(
			new SectionLabel(6, 10, 0, "Potions"),
			new SectionLabel(6, 30, 0, "Seeds")
		);

		BankLayoutPlanner.Result plan = BankLayoutPlanner.plan(items, labels, 10, identity);

		assertEquals(10, plan.shiftForY(0));
		assertEquals(10, plan.shiftForY(36));
		assertEquals(20, plan.shiftForY(80));
		assertEquals(20, plan.getExtraScroll());
	}

	@Test
	public void twoLabelsOnOneRowShareASingleGap()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(0, 10, 51, 0),
			slot(1, 11, 99, 0),
			slot(2, 12, 51, 36)
		);
		List<SectionLabel> labels = Arrays.asList(
			new SectionLabel(0, 10, 0, "Herbs"),
			new SectionLabel(0, 11, 0, "Seeds")
		);

		BankLayoutPlanner.Result plan = BankLayoutPlanner.plan(items, labels, 24, identity);

		assertEquals(1, plan.getLabels().size());
		assertEquals("Herbs · Seeds", plan.getLabels().get(0).getText());
		assertEquals(0, plan.getLabels().get(0).getAnchorIndex());
		assertEquals(24, plan.shiftForY(0));
		assertEquals(24, plan.shiftForY(36));
		assertEquals(24, plan.getExtraScroll());
	}

	@Test
	public void missingAnchorDoesNotMoveTheBank()
	{
		List<BankLayoutPlanner.ItemSlot> items = Collections.singletonList(slot(0, 10, 51, 0));
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, 999, 0, "Gone"));

		BankLayoutPlanner.Result plan = BankLayoutPlanner.plan(items, labels, 24, identity);

		assertEquals(0, plan.getExtraScroll());
		assertTrue(plan.getLabels().isEmpty());
		assertEquals(0, plan.shiftForY(0));
	}

	@Test
	public void placeholderUsesTheRealItemId()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(0, PLACEHOLDER_ID, 51, 0),
			slot(1, 300, 51, 36)
		);
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, REAL_ID, 0, "Herbs"));

		BankLayoutPlanner.Result plan = BankLayoutPlanner.plan(items, labels, 18, identity);

		assertEquals(18, plan.shiftForY(0));
		assertEquals("Herbs", plan.getLabels().get(0).getText());
		assertEquals(REAL_ID, BankLayoutPlanner.normalize(PLACEHOLDER_ID, identity));
		assertEquals(REAL_ID, BankLayoutPlanner.normalize(REAL_ID, identity));
	}

	@Test
	public void secondCopyOfAnItemIsASeparateAnchor()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(4, REAL_ID, 99, 0),
			slot(2, REAL_ID, 51, 0),
			slot(9, REAL_ID, 51, 36)
		);
		// Reading order is y then x, so index 2, then 4, then 9.
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, REAL_ID, 2, "Third stack"));

		BankLayoutPlanner.Result plan = BankLayoutPlanner.plan(items, labels, 20, identity);

		assertEquals(0, plan.shiftForY(0));
		assertEquals(20, plan.shiftForY(36));
		assertEquals(9, plan.getLabels().get(0).getAnchorIndex());
		assertEquals(0, BankLayoutPlanner.occurrenceOf(items, 2, identity));
		assertEquals(1, BankLayoutPlanner.occurrenceOf(items, 4, identity));
		assertEquals(2, BankLayoutPlanner.occurrenceOf(items, 9, identity));
	}

	@Test
	public void realItemAndItsPlaceholderShareAnOccurrenceSequence()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(0, REAL_ID, 51, 0),
			slot(1, PLACEHOLDER_ID, 99, 0)
		);

		assertEquals(0, BankLayoutPlanner.occurrenceOf(items, 0, identity));
		assertEquals(1, BankLayoutPlanner.occurrenceOf(items, 1, identity));
	}

	@Test
	public void adjustmentIsSkippedForSearchTagTabsAndPotionStorage()
	{
		assertTrue(BankLayoutPlanner.shouldAdjust(true, false, false, false));
		assertFalse(BankLayoutPlanner.shouldAdjust(false, false, false, false));
		assertFalse(BankLayoutPlanner.shouldAdjust(true, true, false, false));
		assertFalse(BankLayoutPlanner.shouldAdjust(true, false, true, false));
		assertFalse(BankLayoutPlanner.shouldAdjust(true, false, false, true));
	}

	private static BankLayoutPlanner.ItemSlot slot(int index, int itemId, int x, int y)
	{
		return new BankLayoutPlanner.ItemSlot(index, itemId, x, y);
	}
}
