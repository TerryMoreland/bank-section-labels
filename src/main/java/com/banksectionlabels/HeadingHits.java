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

import java.util.List;

/**
 * Heading rows are paint-only. Menus are chosen by testing the mouse against
 * these rectangles so the rows never become widgets that can swallow the wheel.
 */
final class HeadingHits
{
	private HeadingHits()
	{
	}

	static final class Target
	{
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final boolean tabTitle;
		private final int tab;
		private final int sectionIndex;
		private final String text;

		Target(int x, int y, int width, int height, boolean tabTitle, int tab, int sectionIndex, String text)
		{
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.tabTitle = tabTitle;
			this.tab = tab;
			this.sectionIndex = sectionIndex;
			this.text = text == null ? "" : text;
		}

		int getTab()
		{
			return tab;
		}

		int getSectionIndex()
		{
			return sectionIndex;
		}

		boolean isTabTitle()
		{
			return tabTitle;
		}

		String getText()
		{
			return text;
		}

		boolean contains(int pointX, int pointY)
		{
			return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
		}
	}

	static Target find(List<Target> targets, int pointX, int pointY)
	{
		if (targets == null)
		{
			return null;
		}
		for (int i = targets.size() - 1; i >= 0; i--)
		{
			Target target = targets.get(i);
			if (target != null && target.contains(pointX, pointY))
			{
				return target;
			}
		}
		return null;
	}
}
