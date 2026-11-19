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

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;

/**
 * This plugin's own names for vanilla tabs 1–9, saved on the account profile.
 * A saved name wins over Bank Tab Names, which wins over {@code Tab N}.
 */
final class TabTitles
{
	static final String KEY = "tabtitles";

	private TabTitles()
	{
	}

	static String resolve(ConfigManager configManager, int tab)
	{
		String override = decode(readRaw(configManager)).get(tab);
		String fromBankTabNames = BankTabNames.read(configManager, tab);
		return BankTabNames.resolve(tab, override, fromBankTabNames);
	}

	static String readRaw(ConfigManager configManager)
	{
		if (configManager == null)
		{
			return "";
		}
		try
		{
			String raw = configManager.getRSProfileConfiguration(BankSectionLabelsPlugin.CONFIG_GROUP, KEY);
			return raw == null ? "" : raw;
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	static void save(ConfigManager configManager, Map<Integer, String> titles)
	{
		if (configManager == null)
		{
			return;
		}
		String encoded = encode(titles);
		if (encoded.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(BankSectionLabelsPlugin.CONFIG_GROUP, KEY);
			return;
		}
		configManager.setRSProfileConfiguration(BankSectionLabelsPlugin.CONFIG_GROUP, KEY, encoded);
	}

	static Map<Integer, String> withTitle(Map<Integer, String> existing, int tab, String title)
	{
		Map<Integer, String> next = new LinkedHashMap<>();
		if (existing != null)
		{
			next.putAll(existing);
		}
		String clean = clean(title);
		if (tab < 1 || tab > 9 || clean.isEmpty())
		{
			next.remove(tab);
		}
		else
		{
			next.put(tab, clean);
		}
		return next;
	}

	static Map<Integer, String> decode(String raw)
	{
		Map<Integer, String> titles = new LinkedHashMap<>();
		if (raw == null || raw.isEmpty())
		{
			return titles;
		}
		for (String line : raw.split("\n", -1))
		{
			if (line.isEmpty())
			{
				continue;
			}
			int split = line.indexOf('\t');
			if (split <= 0)
			{
				continue;
			}
			try
			{
				int tab = Integer.parseInt(line.substring(0, split).trim());
				String name = clean(line.substring(split + 1));
				if (tab >= 1 && tab <= 9 && !name.isEmpty())
				{
					titles.put(tab, name);
				}
			}
			catch (NumberFormatException ex)
			{
				// Skip one damaged line.
			}
		}
		return titles;
	}

	static String encode(Map<Integer, String> titles)
	{
		if (titles == null || titles.isEmpty())
		{
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (Map.Entry<Integer, String> entry : titles.entrySet())
		{
			if (entry.getKey() == null || entry.getKey() < 1 || entry.getKey() > 9)
			{
				continue;
			}
			String name = clean(entry.getValue());
			if (name.isEmpty())
			{
				continue;
			}
			if (out.length() > 0)
			{
				out.append('\n');
			}
			out.append(entry.getKey()).append('\t').append(name);
		}
		return out.toString();
	}

	static String clean(String raw)
	{
		if (raw == null)
		{
			return "";
		}
		String text = raw.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim().replaceAll(" +", " ");
		if (text.length() > 40)
		{
			text = text.substring(0, 40).trim();
		}
		return text;
	}
}
