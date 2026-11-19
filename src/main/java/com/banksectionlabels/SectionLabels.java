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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes section labels for {@code ConfigManager} RS-profile storage.
 * One label per line: {@code tab, item id, occurrence, text}, separated by tabs.
 */
public final class SectionLabels
{
	public static final int MAX_TEXT_LENGTH = 60;

	private SectionLabels()
	{
	}

	public static String sanitize(String raw)
	{
		if (raw == null || raw.isEmpty())
		{
			return "";
		}

		// Drop <col=...> style tags so a pasted color code cannot leak into the heading.
		raw = raw.replaceAll("<[^>]*>", "");

		StringBuilder out = new StringBuilder(Math.min(raw.length(), MAX_TEXT_LENGTH));
		for (int i = 0; i < raw.length() && out.length() < MAX_TEXT_LENGTH; i++)
		{
			char c = raw.charAt(i);
			if (c == '\n' || c == '\r' || c == '\t')
			{
				c = ' ';
			}
			if (c < 32 || c == 127 || c == '<' || c == '>')
			{
				continue;
			}
			out.append(c);
		}
		return out.toString().trim();
	}

	public static String encode(List<SectionLabel> labels)
	{
		List<SectionLabel> sorted = new ArrayList<>(dedupe(labels));
		sorted.sort(Comparator
			.comparingInt(SectionLabel::getTab)
			.thenComparingInt(SectionLabel::getItemId)
			.thenComparingInt(SectionLabel::getOccurrence));

		StringBuilder out = new StringBuilder();
		for (SectionLabel label : sorted)
		{
			if (out.length() > 0)
			{
				out.append('\n');
			}
			out.append(label.getTab())
				.append('\t')
				.append(label.getItemId())
				.append('\t')
				.append(label.getOccurrence())
				.append('\t')
				.append(label.getText());
		}
		return out.toString();
	}

	public static List<SectionLabel> decode(String raw)
	{
		List<SectionLabel> labels = new ArrayList<>();
		if (raw == null || raw.isEmpty())
		{
			return labels;
		}

		String[] lines = raw.split("\n", -1);
		for (String line : lines)
		{
			if (line.isEmpty())
			{
				continue;
			}
			String[] parts = line.split("\t", 4);
			if (parts.length < 4)
			{
				continue;
			}
			try
			{
				int tab = Integer.parseInt(parts[0].trim());
				int itemId = Integer.parseInt(parts[1].trim());
				int occurrence = Integer.parseInt(parts[2].trim());
				String text = sanitize(parts[3]);
				if (tab < 0 || tab > 20 || itemId < 0 || occurrence < 0 || text.isEmpty())
				{
					continue;
				}
				labels.add(new SectionLabel(tab, itemId, occurrence, text));
			}
			catch (NumberFormatException ex)
			{
				// Skip a damaged line and keep the rest.
			}
		}
		return dedupe(labels);
	}

	public static List<SectionLabel> forTab(List<SectionLabel> labels, int tab)
	{
		List<SectionLabel> matched = new ArrayList<>();
		for (SectionLabel label : labels)
		{
			if (label.getTab() == tab)
			{
				matched.add(label);
			}
		}
		return matched;
	}

	public static String textFor(List<SectionLabel> labels, int tab, int itemId, int occurrence)
	{
		String text = null;
		for (SectionLabel label : labels)
		{
			if (label.sameAnchor(tab, itemId, occurrence))
			{
				text = label.getText();
			}
		}
		return text;
	}

	public static List<SectionLabel> upsert(List<SectionLabel> labels, SectionLabel updated)
	{
		List<SectionLabel> out = new ArrayList<>();
		boolean wrote = false;
		for (SectionLabel label : labels)
		{
			if (label.sameAnchor(updated.getTab(), updated.getItemId(), updated.getOccurrence()))
			{
				if (!wrote)
				{
					out.add(updated);
					wrote = true;
				}
			}
			else
			{
				out.add(label);
			}
		}
		if (!wrote)
		{
			out.add(updated);
		}
		return out;
	}

	public static List<SectionLabel> remove(List<SectionLabel> labels, int tab, int itemId, int occurrence)
	{
		List<SectionLabel> out = new ArrayList<>();
		for (SectionLabel label : labels)
		{
			if (!label.sameAnchor(tab, itemId, occurrence))
			{
				out.add(label);
			}
		}
		return out;
	}

	private static List<SectionLabel> dedupe(List<SectionLabel> labels)
	{
		Map<String, SectionLabel> byAnchor = new LinkedHashMap<>();
		for (SectionLabel label : labels)
		{
			byAnchor.put(label.getTab() + "\t" + label.getItemId() + "\t" + label.getOccurrence(), label);
		}
		return new ArrayList<>(byAnchor.values());
	}
}
