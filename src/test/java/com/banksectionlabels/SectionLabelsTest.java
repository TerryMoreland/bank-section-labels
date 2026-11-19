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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SectionLabelsTest
{
	@Test
	public void roundTripPreservesText()
	{
		List<SectionLabel> labels = Arrays.asList(
			new SectionLabel(6, 5295, 0, "Seeds/Farming"),
			new SectionLabel(0, 249, 1, "Herbs")
		);

		List<SectionLabel> decoded = SectionLabels.decode(SectionLabels.encode(labels));

		assertEquals(Arrays.asList(
			new SectionLabel(0, 249, 1, "Herbs"),
			new SectionLabel(6, 5295, 0, "Seeds/Farming")
		), decoded);
	}

	@Test
	public void damagedLinesAreSkipped()
	{
		String raw = "not-a-label\n6\t249\t0\tHerbs\n6\tnope\t0\tBroken\n";

		List<SectionLabel> labels = SectionLabels.decode(raw);

		assertEquals(Collections.singletonList(new SectionLabel(6, 249, 0, "Herbs")), labels);
	}

	@Test
	public void sanitizeStripsSeparatorsAndMarkup()
	{
		assertEquals("Herbs Seeds", SectionLabels.sanitize("Herbs\nSeeds"));
		assertEquals("Seeds/Farming", SectionLabels.sanitize("  Seeds/Farming  "));
		assertEquals("bold", SectionLabels.sanitize("<col=ff0000>bold</col>"));
		assertEquals("", SectionLabels.sanitize("   "));
		assertEquals("", SectionLabels.sanitize(null));
		assertTrue(SectionLabels.sanitize(repeat('a', 80)).length() <= SectionLabels.MAX_TEXT_LENGTH);
	}

	@Test
	public void upsertKeepsASecondItemAsItsOwnLabel()
	{
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(0, 590, 0, "Woodcutting and Firemaking"));

		List<SectionLabel> updated = SectionLabels.upsert(labels, new SectionLabel(0, 1511, 0, "Herbs"));

		assertEquals(Arrays.asList(
			new SectionLabel(0, 590, 0, "Woodcutting and Firemaking"),
			new SectionLabel(0, 1511, 0, "Herbs")
		), updated);
	}

	@Test
	public void upsertReplacesTheSameAnchor()
	{
		List<SectionLabel> labels = Collections.singletonList(new SectionLabel(6, 249, 0, "Herbs"));

		List<SectionLabel> updated = SectionLabels.upsert(labels, new SectionLabel(6, 249, 0, "Grimy herbs"));

		assertEquals(Collections.singletonList(new SectionLabel(6, 249, 0, "Grimy herbs")), updated);
		assertEquals("Grimy herbs", SectionLabels.textFor(updated, 6, 249, 0));
		assertNull(SectionLabels.textFor(updated, 6, 249, 1));
	}

	@Test
	public void removeDropsOnlyTheMatchingAnchor()
	{
		List<SectionLabel> labels = Arrays.asList(
			new SectionLabel(6, 249, 0, "Herbs"),
			new SectionLabel(6, 5295, 0, "Seeds")
		);

		List<SectionLabel> left = SectionLabels.remove(labels, 6, 249, 0);

		assertEquals(Collections.singletonList(new SectionLabel(6, 5295, 0, "Seeds")), left);
		assertEquals(Collections.singletonList(new SectionLabel(6, 5295, 0, "Seeds")), SectionLabels.forTab(labels, 6).subList(1, 2));
		assertTrue(SectionLabels.forTab(labels, 0).isEmpty());
	}

	@Test
	public void duplicateAnchorsKeepTheLastText()
	{
		String raw = "6\t249\t0\tOld\n6\t249\t0\tNew";

		assertEquals(Collections.singletonList(new SectionLabel(6, 249, 0, "New")), SectionLabels.decode(raw));
	}

	private static String repeat(char c, int count)
	{
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}
}
