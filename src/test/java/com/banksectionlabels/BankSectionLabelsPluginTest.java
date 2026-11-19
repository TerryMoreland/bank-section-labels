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
import java.util.Arrays;
import java.util.List;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BankSectionLabelsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankSectionLabelsPlugin.class);
		RuneLite.main(withDefaultArgs(args));
	}

	/**
	 * Double-clicking the shaded jar passes no arguments. Developer mode is
	 * required for this launcher, so add the example-plugin flags when the
	 * caller did not.
	 */
	static String[] withDefaultArgs(String[] args)
	{
		List<String> out = new ArrayList<>();
		if (!Arrays.asList(args).contains("--developer-mode"))
		{
			out.add("--developer-mode");
		}
		if (!Arrays.asList(args).contains("--debug"))
		{
			out.add("--debug");
		}
		out.addAll(Arrays.asList(args));
		return out.toArray(new String[0]);
	}

	@Test
	public void addsDeveloperModeWhenMissing()
	{
		assertEquals(
			Arrays.asList("--developer-mode", "--debug", "--safe-mode"),
			Arrays.asList(withDefaultArgs(new String[]{"--safe-mode"}))
		);
	}

	@Test
	public void doesNotDuplicateFlags()
	{
		assertEquals(
			Arrays.asList("--developer-mode", "--debug"),
			Arrays.asList(withDefaultArgs(new String[]{"--developer-mode", "--debug"}))
		);
	}
}
