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
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BankSectionLabelsPlugin.CONFIG_GROUP)
public interface BankSectionLabelsConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Show section labels",
		description = "Draw headings and insert space above labeled rows in regular bank tabs",
		position = 0
	)
	default boolean enabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "requireShift",
		name = "Require Shift for label menu",
		description = "Hold Shift while right-clicking a bank item to add, edit, or remove its section label",
		position = 1
	)
	default boolean requireShift()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "labelColor",
		name = "Label color",
		description = "Color of the heading text and the optional divider line",
		position = 2
	)
	default Color labelColor()
	{
		return new Color(0xFF, 0x90, 0x40);
	}

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "RuneLite font used for headings. Raise the header height if the text is clipped",
		position = 3
	)
	default FontType font()
	{
		return FontType.BOLD;
	}

	@ConfigItem(
		keyName = "divider",
		name = "Divider line",
		description = "Draw a thin line under each heading. There is no filled bar unless Header background is on",
		position = 4
	)
	default boolean divider()
	{
		return true;
	}

	@ConfigItem(
		keyName = "headerBackground",
		name = "Header background",
		description = "Draw a dark bar behind headings. Off leaves plain text, which is the default",
		position = 6
	)
	default boolean headerBackground()
	{
		return false;
	}

	@ConfigItem(
		keyName = "resetSections",
		name = "Reset open tab's sections",
		description = "Turn on to delete every section in the bank tab you are viewing. On the all-items view this clears only untabbed sections. Turns itself off",
		position = 7
	)
	default boolean resetSections()
	{
		return false;
	}

	@Range(min = 14, max = 48)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "headerHeight",
		name = "Header height",
		description = "Vertical space inserted above a labeled row. The heading is drawn inside that gap",
		position = 5
	)
	default int headerHeight()
	{
		return 24;
	}
}
