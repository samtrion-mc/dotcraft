# .Craft

Monorepo of NeoForge mods for Minecraft 26.1.2 (NeoForge 26.1.2.124, Java 25).

## Layout

- `mods/<name>/` - one content mod per directory (`:mods:<name>`, mod id `dotcraft_<name>`, package `net.samtrion.dotcraft.<name>`). Metadata lives in the mod's `gradle.properties` (`mod_id`, `mod_name`, `mod_description`).
- `dev/` - runs all mods together in one game. Every run of every project, also single mods, uses `run/` as game directory (shared saves, config and `run/mods`).
- `build-logic/` - convention plugins (`dotcraft.neoforge-base`, `dotcraft.neoforge-mod`, `dotcraft.root`).
- `.githooks/` - commit message check.

All mods share one version, derived from git tags by git-semver.

## Mods

### .Craft: Resources (`dotcraft_resources`)

The shared material base of the .Craft mods. Other mods build on it instead of each defining their own raw materials.

- **Ores and raw materials:** new ores with world generation, plus the raw items, ingots and dusts they yield.
- **Materials and alloys:** processed materials and alloys that serve as crafting ingredients for machines and equipment in the other mods, including intermediate forms such as plates, gears and rods.
- **Compressed blocks:** 9 compression tiers (9 blocks per tier, up to 9^9) for a curated list of bulk blocks (stones, dirt, sand, gravel, metal and gem blocks, glass, terracotta, concrete, concrete powder, wool, planks and all logs and stems). Craft 3x3 up, 1 down to 9. One shared model; the tier shows as a two-tone (white/black, readable on light and dark blocks) riveted frame that grows with the tier (`tools/generate_overlays.py`). Hardness grows with the tier. Names use one translation key, so no per-block language entries.

### .Craft: Energy (`dotcraft_energy`)

Adds an energy system to the game: power is produced, stored and consumed. Depends on Resources for its materials.

- **Generators:** blocks that produce energy.
- **Storage:** blocks that buffer energy, so production and consumption do not have to run at the same time.
- **Machines:** energy-consuming machines that process materials from Resources.

### Status

Resources: compressed blocks are implemented, the rest is planned. Energy is a scaffold without content. Every mod ships JEI, Jade and REI compat (see below).

## Commands

```
./gradlew build                 # build and check everything
./gradlew :dev:runClient        # all mods + JEI/Jade (REI is disabled for now; -Pdev.mods=dotcraft_resources,dotcraft_energy to limit, -Pdev.noCompat to drop JEI/Jade/REI)
./gradlew :mods:energy:runClient  # a single mod (same run/)
./gradlew newMod --id=magic --name="Magic" --depends=resources [--no-compat]
./gradlew :mods:resources:generateData  # regenerate src/generated/resources (models, lang, recipes, loot, tags); CI fails if it is stale
./gradlew installGitHooks       # enable the commit-msg hook (also runs on IDE sync)
```

## Compressed block sources

The blocks that get compressed are data, not code: `mods/resources/src/main/resources/compressed/*.json`, one file per mod (or topic) in name order. A file names the mod its blocks come from; the blocks are only registered and generated when that mod is loaded, and their recipes and loot tables carry a `mod_loaded` condition.

```json
{
  "mod": "minecraft",
  "bases": [ { "id": "stone", "tool": "pickaxe", "tier": "wood" } ],
  "groups": [ { "each": [ { "tree": "oak" } ], "bases": [ { "id": "${tree}_planks", "tool": "axe" } ] } ]
}
```

`tool` (`pickaxe axe shovel hoe sword shear none`), `tier` (`none wood stone copper gold iron diamond netherite`) and `look` (`cube glass log sided grass`; `grass` needs `bottom` and takes an optional `tint` as `#RRGGBB`) are optional; `side`, `end` (top) and `bottom` name the textures when they cannot be derived from the id; `bottom` defaults to `end` and is not allowed for `log` blocks (they rotate). Supporting another mod means adding its file and, for dev runs, the mod as a `localRuntime` dependency of `:mods:resources` and an optional dependency (`ordering="AFTER"`) in its `neoforge.mods.toml`, so that its blocks register first.

## JEI / Jade / REI compat

Compat is standard and lives inside each content mod (no separate compat projects), under `net.samtrion.dotcraft.<name>.compat.{jei,jade,rei}`. The external mods are `compileOnly` + `localRuntime` dependencies and optional in `neoforge.mods.toml`. The mod class only touches the compat classes behind `ModList.get().isLoaded("jei" | "jade" | "roughlyenoughitems")`. `newMod` generates this skeleton.

## Commits and versioning

Commits must follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>[(scope)][!]: <description>`.

- Types: `feat fix docs style refactor perf test build ci chore revert`
- Scopes: a mod name (`resources`, `energy`, ...), `build`, `deps`, `ci`
- `!` or a `BREAKING CHANGE:` footer marks a breaking change.

The hook (`.githooks/check-message.sh`) is installed by `./gradlew installGitHooks`; the `Commits` workflow checks PR commits and the PR title.

Version bumps follow the commits since the last tag: `fix` -> patch, `feat` -> minor, breaking -> major.

## Release

```
./gradlew releaseVersion
```

Creates the next `v<version>` tag from the commit history. Push the tag to publish.
