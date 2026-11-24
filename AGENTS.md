# Collection Log Luck — Agent Guide

## Project Overview

**Collection Log Luck** is a [RuneLite](https://runelite.net/) plugin (Java 8 source compatibility, Gradle 6.2) that calculates and displays luck/dryness percentiles for Old School RuneScape collection log items directly on the in-game Collection Log interface and via item "Check" chat messages.

- **Standalone mode note**: `collectionlog.net` has shut down. Local player item counts and kill counts are cached directly from in-game Collection Log pages (`ScriptID.COLLECTION_DRAW_LIST`). Do not assume `collectionlog.net` is reachable for live `!luck` lookups unless re-enabled.
- **Dependencies**: Uses `org.apache.commons:commons-math3:3.0`, Lombok, and RuneLite `client` (`latest.release` from `https://repo.runelite.net`). Because of `commons-math3`, `runelite-plugin.properties` must retain `build=gradle`.

## Build & Test

Gradle 6.2 requires **JDK 11** (JDK 17+ will fail to run the wrapper). Ensure `JAVA_HOME` points to a JDK 11 installation before invoking Gradle.

```powershell
# PowerShell example (adjust path to your local JDK 11)
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-11.0.29"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat test --no-daemon
```

- Always run the full unit test suite (`test`) after modifying drop mechanics, probability distributions, config options, or utilities.
- `src/test/java/com/peanubnutter/collectionlogluck/CollectionLogLuckPluginTest.java` is a manual dev launcher (`main` method) that boots RuneLite with this plugin loaded—it is not a JUnit test case.

## Architecture & Key Files

- `src/main/java/com/peanubnutter/collectionlogluck/`
  - `CollectionLogLuckPlugin.java` — Main RuneLite plugin lifecycle, event subscribers (`onGameStateChanged`, `onChatMessage`, `onScriptPostFired`), in-game collection log scraping/caching, and the one-time update banner (`pluginVersion`, `pluginMessage`).
  - `CollectionLogLuckConfig.java` — `@ConfigGroup("collectionlogluck")` settings for overlay appearance, pre-buff KC adjustments, and boss/raid contribution overrides.
  - `CollectionLogWidgetItemOverlay.java` — Renders luck percentile / dry-calc text and colored backgrounds over Collection Log item slots.
- `luck/`
  - `LogItemInfo.java` — Master registry mapping every collection log item (name + OSRS item ID) to its `DropLuck` distribution.
  - `LogItemSourceInfo.java` — Enum of kill count source labels matching exact in-game Collection Log header strings.
  - `CollectionLogItemAliases.java` — Item name aliases/abbreviations for fuzzy matching.
  - `drop/` — `DropLuck` interface, `AbstractDrop`, and drop models (`BinomialDrop`, `PoissonBinomialDrop`, `HiddenShardDrop`, `FiniteBinomialDrop`, `FixedStackDrop`, `BinomialUniformSumDrop`, `DupeProtected*`, `InterchangeableSet*`, `GuaranteedOnceBinomialDrop`, `SinglePityBinomialDrop`, `CappedCollectionLogTrackingDrop`, `DeterministicDrop`, `MissingKillCountDrop`, `UnimplementedDrop`).
  - `probability/` — Custom probability distributions and normal/discrete approximations.
- `model/` & `util/` — Collection log POJOs, deserialization, fuzzy matching (`FuzzyStringMatch`), and luck formatting/color helpers (`LuckUtils`).
- `src/test/java/com/peanubnutter/collectionlogluck/` — JUnit 4 tests for drop models, config options (`DropConfigOptionsTest`), probability distributions, aliases, and utilities.

## Common Workflows

### Adding or Updating Drops
1. If introducing a new boss/activity KC header, add the exact in-game string to `LogItemSourceInfo.java`.
2. Add or update item entries in `LogItemInfo.java` with the appropriate `DropLuck` implementation and `RollInfo` rate(s).
3. If the drop requires a user-configurable KC/contribution/pre-buff offset:
   - Add the config key constant and `@ConfigItem` in `CollectionLogLuckConfig.java`.
   - Wire it via `.withConfigOption(...)` in `LogItemInfo.java` and handle it in `AbstractDrop.java` (or the relevant drop subclass, e.g., `PoissonBinomialDrop.java`).
   - Add coverage in `DropConfigOptionsTest.java` and/or the relevant drop test class.
4. Run `.\gradlew.bat test --no-daemon` to verify all tests pass.

### Verifying New OSRS Items, IDs & KC Headers
- **Exact KC header strings (`LogItemSourceInfo`)**: Never guess whether an activity includes `"The "` or `"kills"` vs. `"completions"`. Check the decompiled OSRS CS2 script `scripts/[proc,collection_category_count].cs2` in `runelite/cs2-scripts` (e.g., via `gh search code`) for the exact Collection Log header text.
- **Exact item IDs & names (`LogItemInfo`)**: Refresh RuneLite dependencies (`.\gradlew.bat build --refresh-dependencies --no-daemon`) and inspect `net.runelite.api.ItemID` / `net.runelite.api.gameval.ItemID` in the downloaded `runelite-api-*.jar` (using `javap -constants`), cross-checking `(uncharged)` / `(inert)` log variants against the OSRS Wiki (`oldschool.runescape.wiki`) or OSRS collection log enum dumps.
- **In-game discovery helpers**: Commented-out logger snippets in `CollectionLogLuckPlugin.cacheCollectionLogPageData()` (missing items) and `CollectionLog.searchForKillCount()` (missing KC headers) can be temporarily uncommented while running `CollectionLogLuckPluginTest` to print missing entries to the console.
- **Drop rates & mechanics**: Verify rates, unique weights, and dupe-protection rules against the official OSRS Wiki (`oldschool.runescape.wiki` / MediaWiki API), not third-party guide sites.

### Preparing a Release
- Bump `pluginVersion` and update `pluginMessage` in `CollectionLogLuckPlugin.java` so players receive the one-time chat changelog on login.
- Merge changes to `master` and update the RuneLite Plugin Hub commit reference when ready to publish.

## Git & Repository Conventions

- Default branch on GitHub is `master`, with active standalone development done on `collection-log-luck-standalone-v1` (merged to `master` via PR for releases).
- Verify local git author config matches the repository identity before committing (`git config --local user.name` / `user.email`).
- Preserve **LF (`\n`)** line endings in Java source files (the repository uses LF with `core.autocrlf=false`).
- Never commit `.gradle/`, `build/`, or IDE metadata.
