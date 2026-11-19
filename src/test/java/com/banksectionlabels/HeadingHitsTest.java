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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HeadingHitsTest
{
	@Test
	public void titleAndSectionRowsAreDistinctFromTheItemRow()
	{
		HeadingHits.Target title = new HeadingHits.Target(51, 40, 384, 28, true, 2, -1, "2 Melee");
		HeadingHits.Target section = new HeadingHits.Target(51, 104, 384, 24, false, 2, 0, "Weapons");
		java.util.List<HeadingHits.Target> rows = Arrays.asList(title, section);

		HeadingHits.Target onTitle = HeadingHits.find(rows, 60, 50);
		assertTrue(onTitle.isTabTitle());
		assertEquals(2, onTitle.getTab());

		// The divider is the last pixel of the title band.
		assertTrue(HeadingHits.find(rows, 200, 67).isTabTitle());

		HeadingHits.Target onSection = HeadingHits.find(rows, 80, 110);
		assertEquals("Weapons", onSection.getText());
		assertEquals(0, onSection.getSectionIndex());

		// The item row under the section heading is not a heading.
		assertNull(HeadingHits.find(rows, 80, 140));
		assertNull(HeadingHits.find(rows, 40, 50));
	}
}
