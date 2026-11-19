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

import java.util.Objects;

/**
 * A heading anchored to one item inside one vanilla bank tab.
 * <p>
 * {@code itemId} is the non-placeholder id. {@code occurrence} counts how many
 * earlier copies of that item are visible in the tab, left to right and top to
 * bottom, so two stacks of the same item can carry different headings.
 */
public final class SectionLabel
{
	private final int tab;
	private final int itemId;
	private final int occurrence;
	private final String text;

	public SectionLabel(int tab, int itemId, int occurrence, String text)
	{
		this.tab = tab;
		this.itemId = itemId;
		this.occurrence = occurrence;
		this.text = text;
	}

	public int getTab()
	{
		return tab;
	}

	public int getItemId()
	{
		return itemId;
	}

	public int getOccurrence()
	{
		return occurrence;
	}

	public String getText()
	{
		return text;
	}

	public boolean sameAnchor(int tab, int itemId, int occurrence)
	{
		return this.tab == tab && this.itemId == itemId && this.occurrence == occurrence;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof SectionLabel))
		{
			return false;
		}
		SectionLabel that = (SectionLabel) other;
		return tab == that.tab
			&& itemId == that.itemId
			&& occurrence == that.occurrence
			&& Objects.equals(text, that.text);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(tab, itemId, occurrence, text);
	}

	@Override
	public String toString()
	{
		return "SectionLabel{tab=" + tab + ", itemId=" + itemId + ", occurrence=" + occurrence + ", text=" + text + "}";
	}
}
