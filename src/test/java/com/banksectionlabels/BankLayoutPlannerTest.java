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
	public void placeholderMatchesTheRealItem()
	{
		assertEquals(REAL_ID, BankLayoutPlanner.normalize(PLACEHOLDER_ID, identity));
		assertEquals(REAL_ID, BankLayoutPlanner.normalize(REAL_ID, identity));
	}

	@Test
	public void secondCopyIsASeparateOccurrence()
	{
		List<BankLayoutPlanner.ItemSlot> items = Arrays.asList(
			new BankLayoutPlanner.ItemSlot(2, REAL_ID, 51, 0),
			new BankLayoutPlanner.ItemSlot(4, REAL_ID, 99, 0),
			new BankLayoutPlanner.ItemSlot(9, PLACEHOLDER_ID, 51, 36)
		);

		assertEquals(0, BankLayoutPlanner.occurrenceOf(items, 2, identity));
		assertEquals(1, BankLayoutPlanner.occurrenceOf(items, 4, identity));
		assertEquals(2, BankLayoutPlanner.occurrenceOf(items, 9, identity));
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
}
