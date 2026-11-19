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
		FontMetrics metrics = graphics.getFontMetrics();
		int header = config.headerHeight();
		Color color = config.labelColor();
		Shape previousClip = graphics.getClip();
		graphics.clip(viewport);
		try
		{
			for (SectionDraw section : drawn)
			{
				drawSection(graphics, metrics, viewport, header, color, section);
			}
		}
		finally
		{
			graphics.setClip(previousClip);
		}
		return null;
	}

	private void drawSection(Graphics2D graphics, FontMetrics metrics, Rectangle viewport, int header, Color color, SectionDraw section)
	{
		Widget anchor = section.anchor;
		if (anchor == null)
		{
			return;
		}

		Rectangle item;
		try
		{
			if (anchor.isHidden())
			{
				return;
			}
			item = anchor.getBounds();
		}
		catch (RuntimeException ex)
		{
			return;
		}

		if (item == null || item.width <= 0 || item.height <= 0)
		{
			return;
		}

		int top = item.y - header;
		int barX = viewport.x + 4;
		int barW = Math.max(1, viewport.width - 8);
		graphics.setColor(new Color(12, 10, 8, 220));
		graphics.fillRect(barX, top, barW, Math.max(1, header - 1));

		int baseline = top + (header - metrics.getHeight()) / 2 + metrics.getAscent();
		if (config.divider())
		{
			baseline -= 1;
		}
		int textX = Math.max(item.x, viewport.x + 8);

		graphics.setColor(new Color(0, 0, 0, 220));
		graphics.drawString(section.text, textX + 1, baseline + 1);
		graphics.setColor(color);
		graphics.drawString(section.text, textX, baseline);

		if (config.divider())
		{
			int lineY = item.y - 2;
			graphics.drawLine(barX, lineY, barX + barW, lineY);
		}
	}

	static final class SectionDraw
	{
		final Widget anchor;
		final String text;

		SectionDraw(Widget anchor, String text)
		{
			this.anchor = anchor;
			this.text = text;
		}
	}
}
