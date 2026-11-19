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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BankLayoutPlannerTest
{
	private static final int COLUMNS = 8;
	private static final int START_X = 51;
	private static final int COLUMN_STRIDE = 48;
	private static final int ROW_STRIDE = 36;
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
	public void midRowLabelStartsAFreshRow()
	{
		// Eight items on the first row. The fourth item (column 3) is a section head.
		List<BankLayoutPlanner.ItemSlot> items = row(0, 0, 8);
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, 3, 0, "Herbs"));

		BankLayoutPlanner.Result plan = plan(items, labels, 24);

		assertEquals(xy(0, 0), at(plan, 0));
		assertEquals(xy(1, 0), at(plan, 1));
		assertEquals(xy(2, 0), at(plan, 2));
		// The rest of that row stays empty. Herbs starts at column 0 under the gap.
		assertEquals(xy(0, ROW_STRIDE + 24), at(plan, 3));
		assertEquals(xy(1, ROW_STRIDE + 24), at(plan, 4));
		assertEquals(xy(2, ROW_STRIDE + 24), at(plan, 5));
		assertEquals(xy(3, ROW_STRIDE + 24), at(plan, 6));
		assertEquals(xy(4, ROW_STRIDE + 24), at(plan, 7));
		assertEquals(1, plan.getLabels().size());
		assertEquals(3, plan.getLabels().get(0).getAnchorIndex());
		assertEquals("Herbs", plan.getLabels().get(0).getText());
		assertTrue(plan.placementFor(3).isSectionHead());
	}

	@Test
	public void twoLabelsEachStartTheirOwnRow()
	{
		List<BankLayoutPlanner.ItemSlot> items = new ArrayList<>();
		items.addAll(row(0, 0, 8));
		items.addAll(row(8, 1, 4));
		List<SectionLabel> labels = Arrays.asList(
			new SectionLabel(0, 3, 0, "Woodcutting"),
			new SectionLabel(0, 10, 0, "Firemaking")
		);

		BankLayoutPlanner.Result plan = plan(items, labels, 20);

		assertEquals(xy(0, 0), at(plan, 0));
		assertEquals(xy(2, 0), at(plan, 2));
		int herbsY = ROW_STRIDE + 20;
		assertEquals(xy(0, herbsY), at(plan, 3));
		assertEquals(xy(1, herbsY), at(plan, 4));
		// Item 10 was column 2 of the second vanilla row. It now starts a new row.
		int fireY = herbsY + ROW_STRIDE + 20;
		assertEquals(xy(0, fireY), at(plan, 10));
		assertEquals(xy(1, fireY), at(plan, 11));
		assertEquals(2, plan.getLabels().size());
		assertEquals("Woodcutting", plan.getLabels().get(0).getText());
		assertEquals("Firemaking", plan.getLabels().get(1).getText());
		assertEquals(3, plan.getLabels().get(0).getAnchorIndex());
		assertEquals(10, plan.getLabels().get(1).getAnchorIndex());
	}

	@Test
	public void labelAlreadyAtColumnZeroOnlyInsertsTheGap()
	{
		List<BankLayoutPlanner.ItemSlot> items = new ArrayList<>();
		items.addAll(row(0, 0, 8));
		items.addAll(row(8, 1, 2));
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, 8, 0, "Seeds"));

		BankLayoutPlanner.Result plan = plan(items, labels, 16);

		assertEquals(xy(7, 0), at(plan, 7));
		assertEquals(xy(0, ROW_STRIDE + 16), at(plan, 8));
		assertEquals(xy(1, ROW_STRIDE + 16), at(plan, 9));
	}

	@Test
	public void itemsAfterAHeadFillFollowingRowsBeforeTheNextHead()
	{
		List<BankLayoutPlanner.ItemSlot> items = row(0, 0, 8);
		// Label the second item, then 10 more items would be needed to wrap.
		// With only 8 items, the head is item 1 and six items follow on that row.
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, 1, 0, "Ores"));

		BankLayoutPlanner.Result plan = plan(items, labels, 10);

		assertEquals(xy(0, 0), at(plan, 0));
		int rowY = ROW_STRIDE + 10;
		assertEquals(xy(0, rowY), at(plan, 1));
		assertEquals(xy(6, rowY), at(plan, 7));
	}

	@Test
	public void placeholderHeadStartsItsOwnRow()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(0, 100, START_X, 0),
			slot(1, PLACEHOLDER_ID, START_X + COLUMN_STRIDE, 0),
			slot(2, 300, START_X + 2 * COLUMN_STRIDE, 0)
		);
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(0, REAL_ID, 0, "Guam"));

		BankLayoutPlanner.Result plan = plan(items, labels, 18);

		assertEquals(xy(0, 0), at(plan, 0));
		assertEquals(xy(0, ROW_STRIDE + 18), at(plan, 1));
		assertEquals(xy(1, ROW_STRIDE + 18), at(plan, 2));
		assertEquals("Guam", plan.getLabels().get(0).getText());
	}

	@Test
	public void allItemsTabKeepsASeparatorGap()
	{
		// Second vanilla row starts 20px lower than a normal stride because a tab separator sat between them.
		List<BankLayoutPlanner.ItemSlot> items = new ArrayList<>();
		items.addAll(row(0, 0, 8));
		int separatedY = ROW_STRIDE + 20;
		for (int column = 0; column < 3; column++)
		{
			items.add(slot(8 + column, 100 + column, START_X + column * COLUMN_STRIDE, separatedY));
		}
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(0, 0, 0, "Tab start"));

		BankLayoutPlanner.Result plan = plan(items, labels, 12);

		// First item is already column 0, so it only gains the header.
		assertEquals(xy(0, 12), at(plan, 0));
		// The separator's extra 20px is still in front of the next vanilla row.
		int nextRow = 12 + ROW_STRIDE + 20;
		assertEquals(xy(0, nextRow), at(plan, 8));
		// A 16px separator that used to sit on the vanilla row boundary is placed just above the next group.
		assertEquals(nextRow - 16, plan.yForNonItem(ROW_STRIDE, 16));
	}

	@Test
	public void missingAnchorDoesNotMoveTheBank()
	{
		List<BankLayoutPlanner.ItemSlot> items = row(0, 0, 4);
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, 999, 0, "Gone"));

		BankLayoutPlanner.Result plan = plan(items, labels, 24);

		assertTrue(plan.isEmpty());
		assertNull(plan.placementFor(0));
	}

	@Test
	public void secondCopyOfAnItemIsASeparateAnchor()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(4, REAL_ID, START_X + COLUMN_STRIDE, 0),
			slot(2, REAL_ID, START_X, 0),
			slot(9, REAL_ID, START_X, ROW_STRIDE)
		);
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, REAL_ID, 2, "Third stack"));

		BankLayoutPlanner.Result plan = plan(items, labels, 20);

		assertEquals(xy(0, 0), at(plan, 2));
		assertEquals(xy(1, 0), at(plan, 4));
		assertEquals(xy(0, ROW_STRIDE + 20), at(plan, 9));
		assertEquals(0, BankLayoutPlanner.occurrenceOf(items, 2, identity));
		assertEquals(1, BankLayoutPlanner.occurrenceOf(items, 4, identity));
		assertEquals(2, BankLayoutPlanner.occurrenceOf(items, 9, identity));
	}

	@Test
	public void realItemAndItsPlaceholderShareAnOccurrenceSequence()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			slot(0, REAL_ID, START_X, 0),
			slot(1, PLACEHOLDER_ID, START_X + COLUMN_STRIDE, 0)
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

	private BankLayoutPlanner.Result plan(List<BankLayoutPlanner.ItemSlot> items, List<SectionLabel> labels, int headerHeight)
	{
		return BankLayoutPlanner.plan(items, labels, headerHeight, identity, START_X + COLUMNS * COLUMN_STRIDE);
	}

	private static List<BankLayoutPlanner.ItemSlot> row(int firstIndex, int row, int count)
	{
		List<BankLayoutPlanner.ItemSlot> slots = new ArrayList<>();
		for (int column = 0; column < count; column++)
		{
			int index = firstIndex + column;
			slots.add(slot(index, index, START_X + column * COLUMN_STRIDE, row * ROW_STRIDE));
		}
		return slots;
	}

	private static BankLayoutPlanner.ItemSlot slot(int index, int itemId, int x, int y)
	{
		return new BankLayoutPlanner.ItemSlot(index, itemId, x, y);
	}

	private static String at(BankLayoutPlanner.Result plan, int index)
	{
		BankLayoutPlanner.Placement placement = plan.placementFor(index);
		return placement.getX() + "," + placement.getY();
	}

	private static String xy(int column, int y)
	{
		return (START_X + column * COLUMN_STRIDE) + "," + y;
	}
}
