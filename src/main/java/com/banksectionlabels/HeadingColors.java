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
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;

/**
 * Per-tab and per-section heading colors, stored beside the section layouts.
 * A missing entry falls back to the config default. Old saves have no color lines.
 */
final class HeadingColors
{
	static final String TAB_KEY = "tabcolors";
	static final String SECTION_KEY = "sectioncolors";

	static final class Swatch
	{
		final String name;
		final Color color;

		Swatch(String name, Color color)
		{
			this.name = name;
			this.color = color;
		}
	}

	static final Swatch[] PALETTE = {
		new Swatch("Yellow", new Color(255, 255, 0)),
		new Swatch("Orange", new Color(255, 144, 64)),
		new Swatch("Red", new Color(255, 48, 48)),
		new Swatch("Green", new Color(64, 255, 64)),
		new Swatch("Cyan", new Color(0, 255, 255)),
		new Swatch("Blue", new Color(80, 140, 255)),
		new Swatch("Purple", new Color(192, 96, 255)),
		new Swatch("Pink", new Color(255, 105, 180)),
		new Swatch("White", new Color(255, 255, 255)),
		new Swatch("Gray", new Color(160, 160, 160))
	};

	private HeadingColors()
	{
	}

	static Color resolveTab(ConfigManager configManager, int tab, Color fallback)
	{
		Color saved = decodeTabs(read(configManager, TAB_KEY)).get(tab);
		return saved != null ? saved : fallback;
	}

	static Color resolveSection(ConfigManager configManager, int tab, String name, Color fallback)
	{
		Color saved = decodeSections(read(configManager, SECTION_KEY)).get(sectionKey(tab, name));
		return saved != null ? saved : fallback;
	}

	static void saveTab(ConfigManager configManager, int tab, Color color)
	{
		Map<Integer, Color> colors = decodeTabs(read(configManager, TAB_KEY));
		if (color == null)
		{
			colors.remove(tab);
		}
		else if (tab >= 1 && tab <= 9)
		{
			colors.put(tab, color);
		}
		write(configManager, TAB_KEY, encodeTabs(colors));
	}

	static void saveSection(ConfigManager configManager, int tab, String name, Color color)
	{
		Map<String, Color> colors = decodeSections(read(configManager, SECTION_KEY));
		String key = sectionKey(tab, name);
		if (key.isEmpty())
		{
			return;
		}
		if (color == null)
		{
			colors.remove(key);
		}
		else
		{
			colors.put(key, color);
		}
		write(configManager, SECTION_KEY, encodeSections(colors));
	}

	static void renameSection(ConfigManager configManager, int tab, String from, String to)
	{
		Map<String, Color> colors = decodeSections(read(configManager, SECTION_KEY));
		String oldKey = sectionKey(tab, from);
		String newKey = sectionKey(tab, to);
		if (oldKey.isEmpty() || newKey.isEmpty() || oldKey.equals(newKey))
		{
			return;
		}
		Color color = colors.remove(oldKey);
		if (color == null)
		{
			return;
		}
		colors.put(newKey, color);
		write(configManager, SECTION_KEY, encodeSections(colors));
	}

	static Color parse(String raw)
	{
		if (raw == null)
		{
			return null;
		}
		String text = raw.trim();
		if (text.startsWith("#"))
		{
			text = text.substring(1);
		}
		if (text.length() == 3)
		{
			text = "" + text.charAt(0) + text.charAt(0)
				+ text.charAt(1) + text.charAt(1)
				+ text.charAt(2) + text.charAt(2);
		}
		try
		{
			if (text.length() == 6)
			{
				int rgb = Integer.parseInt(text, 16);
				return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
			}
			if (text.length() == 8)
			{
				long argb = Long.parseLong(text, 16);
				return new Color((int) ((argb >> 16) & 0xFF), (int) ((argb >> 8) & 0xFF), (int) (argb & 0xFF), (int) ((argb >> 24) & 0xFF));
			}
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
		return null;
	}

	static String format(Color color)
	{
		if (color == null)
		{
			return "";
		}
		if (color.getAlpha() != 255)
		{
			return String.format("#%02X%02X%02X%02X", color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
		}
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	static Map<Integer, Color> decodeTabs(String raw)
	{
		Map<Integer, Color> colors = new LinkedHashMap<>();
		if (raw == null || raw.isEmpty())
		{
			return colors;
		}
		for (String line : raw.split("\n", -1))
		{
			int split = line.indexOf('\t');
			if (split <= 0)
			{
				continue;
			}
			try
			{
				int tab = Integer.parseInt(line.substring(0, split).trim());
				Color color = parse(line.substring(split + 1));
				if (tab >= 1 && tab <= 9 && color != null)
				{
					colors.put(tab, color);
				}
			}
			catch (NumberFormatException ex)
			{
				// Skip one damaged line.
			}
		}
		return colors;
	}

	static String encodeTabs(Map<Integer, Color> colors)
	{
		if (colors == null || colors.isEmpty())
		{
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (Map.Entry<Integer, Color> entry : colors.entrySet())
		{
			if (entry.getKey() == null || entry.getValue() == null || entry.getKey() < 1 || entry.getKey() > 9)
			{
				continue;
			}
			if (out.length() > 0)
			{
				out.append('\n');
			}
			out.append(entry.getKey()).append('\t').append(format(entry.getValue()));
		}
		return out.toString();
	}

	static Map<String, Color> decodeSections(String raw)
	{
		Map<String, Color> colors = new LinkedHashMap<>();
		if (raw == null || raw.isEmpty())
		{
			return colors;
		}
		for (String line : raw.split("\n", -1))
		{
			String[] parts = line.split("\t", 3);
			if (parts.length < 3)
			{
				continue;
			}
			try
			{
				int tab = Integer.parseInt(parts[0].trim());
				Color color = parse(parts[2]);
				String key = sectionKey(tab, parts[1]);
				if (!key.isEmpty() && color != null)
				{
					colors.put(key, color);
				}
			}
			catch (NumberFormatException ex)
			{
				// Skip one damaged line.
			}
		}
		return colors;
	}

	static String encodeSections(Map<String, Color> colors)
	{
		if (colors == null || colors.isEmpty())
		{
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, Color> entry : colors.entrySet())
		{
			if (entry.getKey() == null || entry.getValue() == null || entry.getKey().isEmpty())
			{
				continue;
			}
			if (out.length() > 0)
			{
				out.append('\n');
			}
			out.append(entry.getKey()).append('\t').append(format(entry.getValue()));
		}
		return out.toString();
	}

	static String sectionKey(int tab, String name)
	{
		String clean = name == null ? "" : name.replace('\t', ' ').replace('\n', ' ').trim();
		if (tab < 0 || clean.isEmpty())
		{
			return "";
		}
		return tab + "\t" + clean;
	}

	private static String read(ConfigManager configManager, String key)
	{
		if (configManager == null)
		{
			return "";
		}
		try
		{
			String raw = configManager.getRSProfileConfiguration(BankSectionLabelsPlugin.CONFIG_GROUP, key);
			return raw == null ? "" : raw;
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	private static void write(ConfigManager configManager, String key, String encoded)
	{
		if (configManager == null)
		{
			return;
		}
		if (encoded == null || encoded.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(BankSectionLabelsPlugin.CONFIG_GROUP, key);
			return;
		}
		configManager.setRSProfileConfiguration(BankSectionLabelsPlugin.CONFIG_GROUP, key, encoded);
	}
}
