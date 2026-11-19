# Bank Section Labels

A RuneLite plugin that draws your own text headings above groups of items in the **regular bank** (the vanilla tabs, including the main “all items” tab). It is not a Bank Tags layout. Headings stay stuck to the item you labeled, and the row moves down so the text sits in a real gap instead of covering the icons.

Example, on tab 6: a heading **Herbs** above the row where the herbs start, then **Seeds/Farming** above a later row.

## How to use it

1. Open your bank.
2. Hold **Shift** and right-click the item that starts a group.
3. Choose **Add section label**.
4. Type the heading in the chatbox and press Enter. Leave the box empty and press Enter to remove it.
5. The row that contains that item, and everything below it, shifts down. The heading is drawn in the gap and scrolls with the bank.

Right-click that same item again (still holding Shift) for **Edit section label** or **Remove section label**.

Turn **Require Shift for label menu** off in the plugin settings if you want those options on a normal right-click.

### What a label sticks to

A label belongs to one vanilla bank tab plus the item you clicked.

- Tabs 1–9 keep their own labels. The main “all items” view is tab 0 and has its own labels, because the all-items view inserts the game’s tab separators and the rows are not the same.
- If you withdraw or deposit items around the labeled item, the heading follows that item onto whatever row it lands on.
- A placeholder of that item counts as the same item, so releasing a stack down to its placeholder keeps the heading.
- Two stacks of the same item can have different headings. The plugin counts copies from left to right, top to bottom. Labeling the first stack is the stable choice.
- While bank search is open, headings are hidden and the items go back to the normal search layout. Placeholders, withdraw, and deposit are unchanged.
- Bank Tags tag tabs and the potion storage are left alone. Those views are not the vanilla tab grid.

Labels are saved per account with RuneLite’s profile config, so they come back the next time you log in on that account.

### Settings

| Setting | What it does |
| --- | --- |
| Show section labels | Turn the headings and the extra space on or off |
| Require Shift for label menu | Hide the add/edit/remove options unless Shift is held |
| Label color | Text and divider color. Alpha is supported |
| Font | Any RuneLite font, including the RuneScape fonts |
| Divider line | A line under the heading, just above the items |
| Header height | How many pixels of space are inserted above the labeled row |

If a font is clipped, raise the header height. The default (24px) fits the bold RuneScape font.

## How the spacing works

After the bank’s own layout script finishes (`BANKMAIN_FINISHBUILDING`), the plugin looks at the item widgets in the bank container. Items that share a Y coordinate are one row. For each labeled row it adds the header height to that row’s Y and to every widget below it, using `setOriginalY` and `revalidate`. The container’s scroll height grows by the same amount, and the scrollbar script is run again so you can still scroll to the last row.

That is the same idea Bank Tags uses when it places items in a layout (`LayoutManager` sets each item’s original X/Y and revalidates), and the same idea as the older “remove tab separators” code that rewrote `originalY` and then called `UPDATE_SCROLLBAR`. Click targets follow the widgets, because the client hit-tests the widget bounds. The plugin does not change item ids, child indexes, or the withdraw/deposit actions, and the menu entries it adds are client-side only (`MenuAction.RUNELITE`). Nothing extra is sent to the server, and no real bank item is moved.

Headings themselves are drawn by an overlay in the gap, clipped to the item container. Overlay text can use the full RuneLite font list. A text widget inside the bank could not.

## Limitations

- Headings are per vanilla tab. A label made on tab 6 does not also appear on the all-items tab. Add it there separately if you want it in both views.
- Search, Bank Tags tag tabs, and potion storage hide the headings. If a tag tab is open, items are not shifted.
- Duplicate item ids are told apart by visual order. Withdrawing an earlier copy of the same item makes a later copy take its place in that count. Labeling a unique item (the usual section start) does not have this problem.
- Clicks in the empty gap hit the bank background, not an item. Clicks on the items themselves still withdraw and deposit.
- Another plugin that moves bank item widgets later in the same frame can fight this one. This plugin runs after the normal bank build and after Bank Tags’ layout pass.
- Very large header heights, or a heading on many rows, make the bank scroll further. That is the inserted space.
- The gap is not available while the bank is being searched, so you cannot edit labels from search results. Close search, then Shift-right-click the item.

## Requirements

- JDK 11 or newer (JDK 17 is fine)
- A desktop RuneLite install. RuneLite does not run on mobile.

## Run it in the development client

From the project directory:

```bash
./gradlew run
```

On Windows, use `gradlew.bat run`.

The Gradle `run` task is the example-plugin launcher. It starts RuneLite with `--developer-mode` and `--debug`, and loads this plugin directly. You do not need to copy a jar for this path.

Log in with a Jagex account using the steps at <https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts>. Then open a bank and add a heading.

Enable **Bank Section Labels** in the plugin list if it is not already on. The plugin starts enabled.

`./gradlew build` compiles the plugin and runs the unit tests. `./gradlew run` is what opens the client.

## Sideload a jar into an installed client

Sideloading is only for development. It requires the official client to be started in developer mode, and the jar has to be built against a compatible RuneLite version. Rebuild after the client updates.

```bash
./gradlew jar
```

Copy `build/libs/bank-section-labels-1.0.0.jar` into the sideload folder:

- Linux and macOS: `~/.runelite/sideloaded-plugins/`
- Windows: `%USERPROFILE%\.runelite\sideloaded-plugins\`

Do not copy `bank-section-labels-1.0.0-all.jar`. That fat jar is the dev launcher, not a plugin.

Start RuneLite with developer mode:

```bash
java -ea -jar RuneLite.jar --developer-mode
```

The exact jar name of the client depends on how you installed it. In IntelliJ, add the program argument `--developer-mode` and the VM option `-ea`, as described in the [client developer tools](https://github.com/runelite/runelite/wiki/Using-the-client-developer-tools) page.

The plugin then appears in the side panel. Developer mode is not meant for normal play.

## Submit it to the Plugin Hub later

The hub build uses `build=standard` in `runelite-plugin.properties`, so this project’s `build.gradle` is replaced during review. Keep the plugin on the client API only (no extra libraries), Java 11, and the BSD license.

Before opening the request:

1. Put your name in the `author=` line of `runelite-plugin.properties`.
2. Make the GitHub repository public.
3. Commit the version you want reviewed.

Then:

1. Fork <https://github.com/runelite/plugin-hub>.
2. Create a branch.
3. Add a new file under `plugins/` (the file name is the plugin id, for example `bank-section-labels`) containing:

```
repository=https://github.com/YOUR_NAME/YOUR_REPO.git
commit=THE_FULL_40_CHARACTER_COMMIT_SHA
```

4. Open a pull request against `runelite/plugin-hub`. The repository URL is the HTTPS clone URL. The commit is the full hash of the commit to publish, from the commit page of your plugin repo.

The current hub instructions are at <https://github.com/runelite/plugin-hub#creating-new-plugins>. Review also checks [Jagex’s third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and RuneLite’s [rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) list. This plugin only reads the bank and moves widgets on the client. It does not add a menu option that sends a game action.
