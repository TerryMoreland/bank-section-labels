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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BankTabNamesTest
{
	@Test
	public void prefixesTheTabNumberAndStripsMarkup()
	{
		assertEquals("2 Melee", BankTabNames.display(2, "Melee"));
		assertEquals("2 Melee gear", BankTabNames.display(2, "2 Melee gear"));
		assertEquals("1 Main", BankTabNames.display(1, "<col=ffcc33>Main</col>"));
		assertEquals("4 Bossing", BankTabNames.display(4, "<col=ffb000>4 Bossing<br>extra"));
		assertEquals("Tab 3", BankTabNames.display(3, "  "));
		assertEquals("Tab 8", BankTabNames.display(8, null));
		assertEquals("2 Ranged", BankTabNames.resolve(2, "Ranged", "Magic"));
		assertEquals("2 Magic", BankTabNames.resolve(2, "  ", "Magic"));
		assertEquals("Tab 2", BankTabNames.resolve(2, "", ""));
	}
}
