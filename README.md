# Bank Section Labels

A RuneLite plugin that draws your own text headings above groups of items in the **regular bank** (the vanilla tabs, including the main “all items” tab). Each section is a grid you arrange yourself, including empty cells, so a Barrows set can sit in one row as helm, body, legs, weapon.

## How to use it

1. Open a normal bank tab, or the main all-items tab. If a Bank Tags tag tab is open, close it first. Sections step aside while a tag tab or a Bank Tag Layout is active.
2. Hold **Shift** and right-click the item that should lead the group. A placeholder counts if that is the only copy you have.
3. Choose **Add section label**, type a name, and press Enter. That item moves to the start of a new row. The heading is in the gap above it, and the rest of the row is empty.
4. Drag other bank items into those empty spaces, or drop them on the leading item. They stay in the cell where you dropped them, including after you close the bank. Withdraw still takes the real item.
5. To line up a set, drop helm, body, legs, and weapon across one row. Leave a cell blank if you want a gap. When a row is full, another empty row appears for the next set.
6. Shift-right-click a different item and add another label. That is a second section and does not replace the first.
7. Shift-right-click the leading item for **Edit section label** or **Remove section**. Shift-right-click any other item in the section for **Remove from section**.

Turn **Require Shift for label menu** off if you want those options on a normal right-click.

The grid is saved per account. It does not depend on the order of items in the real bank, which cannot store an empty slot in the middle of a row.

### What a label sticks to

- A section belongs to the vanilla tab the item is in. Building it on tab 2 and editing it from the all-items view changes the same section.
- On the all-items view, untabbed items stay at the top. Each numbered tab then gets a bold yellow-orange title, then that tab's loose items, then its smaller orange section labels and grids.
- The title is the name you set with **Rename tab title** (right-click the tab button, the title itself, or an item in that tab). If you have not set one, it uses Bank Tab Names, then `Tab N`. Leave the box empty to clear your name. **Show tab titles** turns the titles off.
- Right-click a section heading for **Edit section label**.
- In a numbered tab, holes between items stay. Empty space after the last item, and the spare row under a section, appear only while you are dragging. After you let go, unused holes are removed and the row shrinks back.
- The all-items view packs each section: each brother still has his own row, but holes are not drawn. Turn on **Show empty cells in all-items view** to see them there too.
- A newly deposited item, or one dropped on a tab button or under a tab title without hitting a section cell, sits in that tab's unsectioned area above the sections. It joins a section only when you drop it on a section cell or give it a label.
- Shift-right-click an item in a section and choose **Compact section** to close every hole in that section.
- A placeholder of an item counts as that item, so a section still shows the item if a placeholder is all you have.
- While bank search is open, headings are hidden and the bank is left as the game drew it.
- Bank Tags tag tabs, Bank Tag Layouts, and the potion storage are left alone.

Labels are saved per account with RuneLite’s profile config, so they come back the next time you log in on that account.

### Settings

| Setting | What it does |
| --- | --- |
| Show section labels | Turn the headings and the extra space on or off |
| Require Shift for label menu | Hide the add/edit/remove options unless Shift is held |
| Label color | Text and divider color. Alpha is supported |
| Font | Any RuneLite font, including the RuneScape fonts |
| Divider line | A thin line under the heading. Off leaves text only |
| Header height | How many pixels of space are inserted above the labeled row |
| Header background | A dark bar behind headings. Off by default |
| Reset open tab's sections | Deletes the sections for the tab you are viewing, then turns itself off. On the all-items view this clears only untabbed sections. Shift-right-click an item and choose **Reset section labels for this tab** to clear the tab that item belongs to |
| Show tab titles | Titles above each numbered tab on the all-items view. Off leaves the section labels |

If a font is clipped, raise the header height. The default (24px) fits the bold RuneScape font.

## How the spacing works

Each section stores an ordered list of item ids for that tab, and `-1` means an empty cell. After the bank builds, the plugin moves the real item widgets onto those cells with `setOriginalX`, `setOriginalY`, and `revalidate`, and puts an invisible drop target on every empty cell. The widget’s bank-slot index is not changed, so withdraw and deposit still hit the real item. A drop onto a section cell updates the saved grid and does not send a bank rearrange. The heading is drawn in the reserved gap above that section’s first row, using the same coordinates as the items, on a layer above the bank so it stays visible with 117 HD.

If a Bank Tags tag tab or a Bank Tag Layout is open, the plugin does not move anything.

## Limitations

- Search, Bank Tags tag tabs, and potion storage hide the headings. If a tag tab is open, items are not shifted.
- Duplicate item ids are told apart by visual order. Withdrawing an earlier copy of the same item makes a later copy take its place in that count. Labeling a unique item (the usual section start) does not have this problem.
- A Bank Tags tag tab, or a Bank Tag Layout that is turned on, is left alone. Open a regular bank tab to edit sections.
- Tab titles use the name from the Bank Tab Names plugin when that plugin has one saved (`banktabnames` / `text_N`, or the older `BankTabNames` / `tabNName`). Otherwise the title is `Tab N`.
- Headings are plain text inside the item area. They do not draw a dark bar unless Header background is turned on, and they do not extend over the tab-icon column.
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

## Run the test client without Gradle

Release RuneLite does not load jars from `sideloaded-plugins`, even with `--developer-mode`. Use the shaded test client instead. It is the example-plugin launcher: `BankSectionLabelsPluginTest` calls `ExternalPluginManager.loadBuiltin` and then `RuneLite.main`. `--developer-mode` and `--debug` are added when you do not pass them.

```bash
./gradlew shadowJar
java -ea -jar build/libs/bank-section-labels-1.0.0-all.jar
```

On Windows, Java 11 or newer can run that jar. RuneLite launcher 2.6.7 already ships one at `%LOCALAPPDATA%\RuneLite\jre\bin\java.exe` (Temurin 11.0.19 on 64-bit Windows). Put `Launch Bank Section Labels.bat` in the same folder as the `-all` jar and double-click the bat, or run:

```bat
"%LOCALAPPDATA%\RuneLite\jre\bin\java.exe" -ea -jar bank-section-labels-1.0.0-all.jar --developer-mode
```

Double-clicking the jar itself only works when Windows has a Java 11+ install associated with `.jar` files. The bundled JRE is not that association, so the bat is the reliable path.

The test client uses the same `%USERPROFILE%\.runelite` folder as the release client.

### Logging in with a Jagex account

On client 1.13.7 the game still reads `%USERPROFILE%\.runelite\credentials.properties` (override the file name with `-Drunelite.credentials.path`). It writes that file only when it is started with `--insecure-write-credentials` and the Jagex Launcher has set `JX_ACCESS_TOKEN`, `JX_REFRESH_TOKEN`, `JX_SESSION_ID`, `JX_CHARACTER_ID`, and `JX_DISPLAY_NAME`. A normal RuneLite shortcut does not set those variables.

1. In RuneLite (configure), add the client argument `--insecure-write-credentials`.
2. Start RuneLite once from the Jagex Launcher so those variables exist and the file is written.
3. Remove the argument again. Launch this test client. It reads the same file and does not need the flag.

The dev client can also log in on its own. Open it, use the account button in the sidebar, and choose **Add Jagex Account**. That stores the session in `.runelite\jagex-accounts.dat` and does not need `credentials.properties`. The current steps are at <https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts>.

`bank-section-labels-1.0.0.jar` (no `-all`) is only the plugin classes. The release client will not load it.

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
