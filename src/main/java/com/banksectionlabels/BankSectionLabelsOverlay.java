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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Paints headings in the gap opened above a labeled row.
 * Item widget bounds already include scroll, so the text stays with the row.
 */
class BankSectionLabelsOverlay extends Overlay
{
	private final Client client;
	private final BankSectionLabelsConfig config;
	private volatile List<SectionDraw> sections = Collections.emptyList();

	@Inject
	BankSectionLabelsOverlay(Client client, BankSectionLabelsConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		// Above the bank widgets, including when 117 HD composites the interface.
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_HIGHEST);
	}

	void setSections(List<SectionDraw> sections)
	{
		if (sections == null || sections.isEmpty())
		{
			this.sections = Collections.emptyList();
			return;
		}
		this.sections = Collections.unmodifiableList(new ArrayList<>(sections));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<SectionDraw> drawn = sections;
		if (drawn.isEmpty() || !config.enabled())
		{
			return null;
		}

		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null || container.isHidden())
		{
			return null;
		}

		Rectangle viewport = container.getBounds();
		if (viewport.width <= 0 || viewport.height <= 0)
		{
			return null;
		}

		Font font = config.font().getFont();
		if (font == null)
		{
			font = FontManager.getRunescapeSmallFont();
		}
		graphics.setFont(font);
		int header = config.headerHeight();
		Color color = config.labelColor();
		Rectangle itemArea = itemArea(viewport);
		Shape previousClip = graphics.getClip();
		graphics.setClip(itemArea);
		try
		{
			for (SectionDraw section : drawn)
			{
				drawSection(graphics, font, viewport, itemArea, container, header, color, section);
			}
		}
		finally
		{
			graphics.setClip(previousClip);
		}
		return null;
	}

	/**
	 * The item container's canvas box starts left of the icons and covers the
	 * bank's tab-icon column. Headings stay inside the icon area.
	 */
	private static Rectangle itemArea(Rectangle viewport)
	{
		int left = viewport.x + BankLayoutPlanner.START_X - 2;
		int right = viewport.x + viewport.width - 8;
		if (left < viewport.x)
		{
			left = viewport.x;
		}
		if (right > viewport.x + viewport.width)
		{
			right = viewport.x + viewport.width;
		}
		if (right <= left + 16)
		{
			return new Rectangle(viewport);
		}
		return new Rectangle(left, viewport.y, right - left, viewport.height);
	}

	private void drawSection(Graphics2D graphics, Font baseFont, Rectangle viewport, Rectangle itemArea, Widget container, int header, Color color, SectionDraw section)
	{
		if (section == null || section.text == null || container == null)
		{
			return;
		}
		net.runelite.api.Point origin;
		try
		{
			origin = container.getCanvasLocation();
		}
		catch (RuntimeException ex)
		{
			return;
		}
		if (origin == null)
		{
			return;
		}

		int scroll = container.getScrollY();
		int top = origin.getY() + section.containerY - scroll;
		if (top + section.height < viewport.y || top > viewport.y + viewport.height)
		{
			return;
		}
		int gap = section.height > 0 ? section.height : header;
		int band = Math.max(1, gap - 2);
		Font useFont = baseFont;
		Color useColor = section.color != null ? section.color : color;
		if (section.tabTitle)
		{
			float size = Math.min(20f, baseFont.getSize2D() + 3f);
			useFont = baseFont.deriveFont(Font.BOLD, size);
		}
		graphics.setFont(useFont);
		FontMetrics metrics = graphics.getFontMetrics();

		if (config.headerBackground())
		{
			graphics.setColor(new Color(12, 10, 8, 200));
			graphics.fillRect(itemArea.x, top, itemArea.width, band);
		}

		int baseline = top + (band - metrics.getHeight()) / 2 + metrics.getAscent();
		int textX = origin.getX() + section.textX;
		if (textX < itemArea.x)
		{
			textX = itemArea.x;
		}

		graphics.setColor(new Color(0, 0, 0, 220));
		graphics.drawString(section.text, textX + 1, baseline + 1);
		graphics.setColor(useColor);
		graphics.drawString(section.text, textX, baseline);

		if (config.divider())
		{
			int lineY = top + band - 1;
			Color chosen = config.dividerColor();
			Color line = new Color(chosen.getRed(), chosen.getGreen(), chosen.getBlue(), Math.min(255, chosen.getAlpha()));
			graphics.setColor(line);
			graphics.drawLine(itemArea.x, lineY, itemArea.x + itemArea.width - 1, lineY);
		}
	}

	static final class SectionDraw
	{
		final int containerY;
		final int height;
		final int textX;
		final String text;
		final boolean tabTitle;
		final Color color;

		SectionDraw(int containerY, int height, int textX, String text, boolean tabTitle, Color color)
		{
			this.containerY = containerY;
			this.height = height;
			this.textX = textX;
			this.text = text;
			this.tabTitle = tabTitle;
			this.color = color;
		}
	}
}
