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

import net.runelite.client.config.ConfigManager;

/**
 * Reads the Bank Tab Names plugin's saved title for a vanilla tab.
 * Current keys are {@code banktabnames}/{@code text_N}. The older plugin used
 * {@code BankTabNames}/{@code tabNName}.
 */
final class BankTabNames
{
	static final String GROUP = "banktabnames";
	static final String LEGACY_GROUP = "BankTabNames";

	private BankTabNames()
	{
	}

	static String heading(ConfigManager configManager, int tab)
	{
		return display(tab, read(configManager, tab));
	}

	static String read(ConfigManager configManager, int tab)
	{
		if (configManager == null || tab <= 0)
		{
			return "";
		}
		try
		{
			String text = configManager.getConfiguration(GROUP, "text_" + tab);
			if (text == null || text.trim().isEmpty())
			{
				text = configManager.getConfiguration(LEGACY_GROUP, "tab" + tab + "Name");
			}
			return text == null ? "" : text;
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	/**
	 * {@code "Melee"} on tab 2 becomes {@code "2 Melee"}. A name that already
	 * starts with that number is left alone. No name falls back to {@code "Tab N"}.
	 */
	static String display(int tab, String configured)
	{
		String plain = plainText(configured);
		if (plain.isEmpty())
		{
			return "Tab " + tab;
		}
		String prefix = tab + " ";
		if (plain.regionMatches(true, 0, prefix, 0, prefix.length())
			|| plain.equalsIgnoreCase("Tab " + tab))
		{
			return plain;
		}
		return prefix + plain;
	}

	static String plainText(String raw)
	{
		if (raw == null || raw.isEmpty())
		{
			return "";
		}
		String first = raw.split("(?i)<br>|\\r?\\n", 2)[0];
		first = first.replaceAll("<[^>]*>", "");
		first = first.replace('\t', ' ').trim().replaceAll(" +", " ");
		if (first.length() > 40)
		{
			first = first.substring(0, 40).trim();
		}
		return first;
	}
}
