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

import java.awt.Color;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HeadingColorsTest
{
	@Test
	public void parsesHexAndRoundTripsSavedColors()
	{
		assertEquals(new Color(255, 153, 0), HeadingColors.parse("#FF9900"));
		assertEquals(new Color(255, 153, 0), HeadingColors.parse("ff9900"));
		assertEquals(new Color(255, 153, 0), HeadingColors.parse("#F90"));
		assertEquals(new Color(255, 153, 0, 128), HeadingColors.parse("#80FF9900"));
		assertNull(HeadingColors.parse("orange"));
		assertNull(HeadingColors.parse("#12"));

		Map<Integer, Color> tabs = HeadingColors.decodeTabs("2\t#FF9900\n4\t#00FFFF");
		assertEquals(new Color(255, 153, 0), tabs.get(2));
		assertEquals("2\t#FF9900\n4\t#00FFFF", HeadingColors.encodeTabs(tabs));

		String sectionLine = "1\tWeapons\t#40FF40";
		Map<String, Color> sections = HeadingColors.decodeSections(sectionLine);
		assertEquals(new Color(64, 255, 64), sections.get(HeadingColors.sectionKey(1, "Weapons")));
		assertEquals(sectionLine, HeadingColors.encodeSections(sections));
	}
}
