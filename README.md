# .Craft

Monorepo of NeoForge mods for Minecraft 26.1.2 (NeoForge 26.1.2.124, Java 25).

## Layout

- `mods/<name>/` - one content mod per directory (`:mods:<name>`, mod id `dotcraft<name>`, package `net.samtrion.dotcraft.<name>`). Metadata lives in the mod's `gradle.properties` (`mod_id`, `mod_name`, `mod_description`).
- `dev/` - runs all mods together in one game. Every run of every project, also single mods, uses `run/` as game directory (shared saves, config and `run/mods`).
- `build-logic/` - convention plugins (`dotcraft.neoforge-base`, `dotcraft.neoforge-mod`, `dotcraft.root`).
- `.githooks/` - commit message check.

All mods share one version, derived from git tags by git-semver.

## Mods

### .Craft: Resources (`dotcraftresources`)

The shared material base of the .Craft mods. Other mods build on it instead of each defining their own raw materials.

- **Ores and raw materials:** new ores with world generation, plus the raw items, ingots and dusts they yield.
- **Materials and alloys:** processed materials and alloys that serve as crafting ingredients for machines and equipment in the other mods, including intermediate forms such as plates, gears, rods and wires and the crafting tools hammer and sandpaper, which stay in the crafting grid and lose durability per craft.
- **Charcoal block:** like the coal block (pickaxe, 16000 ticks of fuel, 3x3 / 1 to 9 charcoal, `c:storage_blocks/charcoal`) and itself a base of the compressed blocks.
- **Compressed blocks:** 9 compression tiers (9 blocks per tier, up to 9^9) for a curated list of bulk blocks (stones, dirt, sand, gravel, metal and gem blocks, glass, terracotta, concrete, concrete powder, wool, planks and all logs and stems). Craft 3x3 up, 1 down to 9. One shared model; the tier shows as a two-tone (white/black, readable on light and dark blocks) riveted frame that grows with the tier. Hardness and the required tool grow with the tier: the base block's requirement for tiers 1-3, one material above for 4-6, two for 7-9 (gold skipped, capped at netherite; e.g. stone needs wood, so tiers 4-6 need stone, 7-9 copper). Blocks that need no tool stay free. Names use one translation key, so no per-block language entries.

### .Craft: Energy (`dotcraftenergy`)

Adds an energy system to the game: power is produced, stored and consumed. Depends on Resources for its materials.

- **Batteries:** potato batteries and tiers 2-5 (items); they charge held items from the inventory.
- **Generators:** blocks that produce energy, five tiers each, that push FE into the neighbours on every side but the top (they cannot be charged). A panel is a flat plate of an eighth of a block (hitbox too), no mob can spawn on it, and it makes nothing while a player, mob or animal stands on it. A panel can be waterlogged, but then it makes only 20 % of its output. Every tier has an own perk besides more FE/t. The peak starts at 15 FE/t (tier 1) and follows the battery ladder, so a panel fills the battery of its tier in well under one MC day; the buffer holds 600 ticks of peak. Every panel is switched on and off in the startup config and tuned in the common config (`generators.<name>.peak/capacity/maxOutput`).
  - **Solar panel:** follows the height of the sun (a smooth curve from sunrise over noon to sunset), so nothing under cover, at night or in a dimension without a sky. Rain cuts it (the weather perk grows per tier, tier 5 is immune), a thunderstorm halves what rain leaves, tiers 3-5 track the sun (wider plateau). It makes nothing at night. Biome tags `solar_boost`/`solar_malus` (x1.15 / x0.85) change it.
  - **Lunar panel:** follows the height of the moon, which is always opposite to the sun, so solar and lunar never make FE at the same time (full moon best, new moon a tier-based floor), the same weather perk and tracking, biome tags `lunar_boost`/`lunar_malus`, and block light above it (light pollution) takes up to half.
  - **Vibration generator:** a full block (five tiers) that listens to the shocks of the world like a sculk sensor does: block breaking and placing, explosions, deaths, damage, lightning, landings and steps (the data tag `dotcraftenergy:vibration_sources`; doors, containers, note blocks, pistons, mounts, equipment, fluids, teleports and projectiles do not count). A louder event gives more FE (by its vibration frequency). A hard cap, a token bucket that refills with a quarter of the peak per tick and holds 20 ticks of peak, makes sure no amount of events, from a player or from a machine that loops an event (a grinder, a dispenser, a pen of mobs), yields more than a quarter of the peak on average: about 4.5-5 batteries of its tier per day at most, the order of a panel. A player who mines or fights all day reaches that ceiling, light play (an event every 100 ticks) about 1.4 batteries. It starts with a full bucket. Sneaking, spectators and wool (also on the block that is hit) are not heard, like at the sculk sensor; wool between the event and the generator stops it up to tier 3. Perk per tier: radius 5 / 7 / 9 / 12 / 16 blocks and fainter shocks: tier 1 the loud ones (blocks, deaths, explosions, lightning), tier 2 also damage, tier 3 also landings, tier 4 hears through wool, tier 5 also steps. The item tooltip names what it needs and hears, a comparator reads the fill of the buffer, and sparks show a heard event. It is no panel and does not join the pool of the panels; it pushes FE into the neighbours on every side and can only be drained.
  - **Groups:** solar and lunar panels (any tier) that touch each other sideways in one plane share one pool of FE, up to 256 blocks (16x16): the capacity is the sum of the buffers, production that does not fit into a panel's own buffer goes to the others, and what a neighbour takes comes out of the whole pool, limited by the config of the panel it is taken from. A removed panel hands its FE on to the groups its neighbours fall into; what does not fit is lost. The top of a group is one surface, only the outer edge keeps its border.
  - **Flow generator:** a turbine pod (a full block in a frame, five tiers with the same peak ladder 15 / 75 / 560 / 5600 / 56000 FE/t) that only runs while it is waterlogged (place it in water or use a bucket on it). It is a sealed pod: it holds water but never spreads it, and another flow generator next to it does not count as water. It has a flow direction (placed facing your look direction, rotates and mirrors): front and back are the flow axis, so it pushes FE into the neighbours on the other four sides only (up, down, left, right) and exposes its energy capability only there, never at front or back. Front and back must stay free for the water: if the block at either of them is solid (full collision) and neither water nor replaceable (air, plants, snow layers and waterlogged blocks are fine), the output is 0, and so is the comparator signal; the pod rechecks this with its scan. The rotor model shows the axis with an axle. It does not join the panels' pool, and its comparator signal shows the output. The item tooltip shows the formula and the perk of the tier.
    - Config: `generators.flow_generator_tier_<n>.peak/capacity/maxOutput` in the common config, `enabled` with the same keys in the startup config. The peak is the best case without animals; animals can lift the output up to 1.3 times the peak.
    - Output = peak x water share x biome x (1 + animals). The water share counts the water in the 5x5x5 cube around the pod (a source block 1, flowing water 2) and is full at 100 (tier 1), 75 (tiers 2-3) or 50 (from tier 4), so more water does not help. The biome is 1.0 in the tag `flow_prime` (ocean and river) and less elsewhere. Distinct types of living aquatic animals in the tag `flow_fauna` near it add 6 % each, at most 5 types (+30 %); wild and bucketed animals count alike. The scan runs every 30 ticks, staggered by position, and at once when the pod is filled or drained.
    - Perks: tier 1 is the baseline without a perk (it needs a nearly full cube of water, animals count within 3 blocks); tier 2 counts animals within 6 blocks; tier 3 keeps 75 % outside ocean and river (others 50 %); tier 4 is full with only 50 weight of water; tier 5 is independent of the biome.
    - It runs around the clock, so in the best place it makes about three times the daily yield of a solar panel of the same tier, and even in a poor place it fills the battery of its tier in a fraction of one MC day.
  - **Thermoelectric generator:** a full block that makes FE from the temperature difference between a hot and a cold neighbour, day and night and in every dimension (also Nether and End, where sun and moon do nothing). It needs two opposite faces: a heat source on one and a cold source on the other. For each of the three axes the heat level of one face plus the cold level of the opposite face gives the difference (levels 1-3 each, so 2-6, either way round), the best axis counts and the axes do not stack; with only a hot or only a cold side, or with heat and cold on faces that merely touch at an edge, it makes nothing. Heat and cold are block tags (data files, `dotcraftenergy:thermo/heat_1..3`, `thermo/cold_1..3`) that a data pack can extend: lit furnaces and campfires, fire and magma, lava; water and a snow layer, snow, powder snow and ice, packed ice and blue ice. An unlit furnace or campfire is not hot. More sources of the same kind do not add up and nothing is consumed (a lava bucket and a blue ice block are cheap, but one block never makes more than its peak). The tier perk is the efficiency: the difference that gives the full peak falls from 6 (tiers 1 and 2: lava and blue ice) over 5 to 4 (tier 5: for example magma and ice), so the same sources give a higher tier a larger share of its larger peak. The ceilings stay high on purpose: the weakest sources (campfire and a puddle) give a tier only a third (tiers 1-2), 40 % or half (tier 5) of its peak, which is still more than four batteries of its tier per MC day; flowing water and lava count like sources. The item tooltip shows peak and ceiling; the best sources fill the battery of the tier in about 1,340 ticks. A comparator reads the share of the peak (0-15). It is no panel and never joins the pool of the panels.
  - **Melt generator (ice):** burns ice for FE, with no menu. A block of the tag `melt_heat` (lava, fire, soul fire, magma, and lit campfires, soul campfires, furnaces, blast furnaces and smokers) touching the generator heats it; every pulse it melts ice, packed ice or blue ice (tags `melt_fuel/ice`, `melt_fuel/packed_ice`, `melt_fuel/blue_ice`) that touches its other sides into air. The melted block gives a fixed FE (config `melt.*`, default 1 000 / 9 000 / 81 000 FE: packed ice is nine ice and blue ice nine packed ice, so one FE costs the same ice whichever type is burnt). The FE goes into a heat reservoir (600 ticks of peak) that releases into the buffer at most the peak per tick (15 / 75 / 560 / 5 600 / 56 000 FE/t, the battery ladder: a fed generator fills the battery of its tier in about 1 340 ticks); nothing melts while the reservoir is full, and a block bigger than the reservoir is still burnt when it is empty. Tier perks: 1 one block per second, 2 two blocks at once, 3 three blocks twice a second, 4 gives the water of the melted ice to the water tank (one bucket per block, none in the Nether), 5 five blocks four times a second (all five ice sides feed the peak; the higher tiers need packed or blue ice to reach it) and recovers all the water: a melted packed ice block gives as many buckets as it is worth ice (9, blue ice 81 with the default yields). The water goes into an internal tank of 32 buckets (32 000 mB), no water block is placed in the world, and water that does not fit is lost. The tank is exposed as an extract-only fluid handler (pipes and buckets can drain it, nothing can be inserted), is saved with the block and is named in the item tooltip of tiers 4 and 5. While the tank is full the efficiency drops by 75 percent: every melted block only gives a quarter of its FE until water is taken out again. Price: tier 1 at full output burns 360 ice (40 packed ice) per MC day; heat is not used up, so ice is the only running cost, and it is no free loop: a melted block is gone, water has to freeze again, and frosted ice (made without limit by frost walker) is no fuel. Not part of the solar/lunar pool; the block glows while it holds heat, and a comparator reads how full reservoir and buffer are together (1-15). When the block is mined or replaced, the FE of the reservoir and the buffer is handed to the neighbours that take FE and what they do not take is lost, so mine it next to a battery. Tiers 4 and 5 are burst tiers (a whole day at peak would take thousands of blue ice); drain the tank to keep the full efficiency. A neighbour in an unloaded chunk is never touched. Disabling a tier in the startup config also makes the tiers above it uncraftable (each needs the generator below). The ice yields are global for all tiers and therefore in the top-level config section `melt`, not under `generators`.
  - **Mob wheel:** a drum that makes FE from living animals running around it (it is no panel and never joins a panel group; a comparator reads its load 0-15). Every second it looks at the mobs in a zone (radius 2-5 by tier, one block down, two up): a mob counts when its type is in the entity type tag `wheel_runners` (`wheel_runners/strong`, e.g. horses, count double, `wheel_runners/weak`, e.g. rabbits, half; hostile mobs are not in it), it is alive, no baby, on the ground, not riding and not told to sit, has at least half of its health (and counts by its health share), and it really moved at least half a block since the last look. The strongest mobs count first, every further one less (diminishing returns), and a tier counts at most 2 / 3 / 4 / 6 / 8 mobs, so a crowd never makes more than the peak; the peak is reached by a full cage of normal mobs. A mob drives one wheel only, even if zones overlap. Perks: tier 2 a bigger cage, tier 3 small animals count fully, tier 4 softer diminishing returns, tier 5 a flywheel that keeps half of the output per second after the mobs stop. The peaks (15 / 75 / 560 / 5600 / 56000 FE/t) are anchored by a duty cycle of a quarter of the day at full load: every tier then fills the battery of its tier in about 5350 ticks. Recipe: plates and gears of iron, nickel, steel, platinum, iridium, the battery of the tier and a wooden rod (tier 1) or the wheel of the tier below.
  - **Kite wind generator:** a winch block (five tiers, own block entity, not part of the panel groups) and a **kite** item (wool, a wooden rod and string). Right-click the winch with a kite and a real kite entity flies up on a tether (tether 16 / 24 / 32 / 48 / 64 blocks for tier 1-5); an empty hand shows altitude and output, sneaking with an empty hand reels it in, and breaking the block gives the kite back as an item. The kite is **not** bound to the build height: it keeps flying above the top of the world (entities are only removed below the bottom). Output = base x length factor x free air around the kite x weather x wind. The tier fixes only the **base** (64 / 400 / 2600 / 17333 / 128000 FE/t, a flat ladder: the tier mostly buys perks); the **length of the tether is the yield factor**: length factor = effective length / 64 (the reference, the longest tether), where the effective length is what the kite really reaches (its altitude, at most the tether), so the factor grows with the real altitude, up to 1. The wind is a gust that swings between 20 % and 100 % (mean 60 %), so even a kite in the open makes about 60 % of its best case. Every block of the way up is checked (a block in the way stops the kite below it, however thin the plate), nothing is read from unloaded chunks, a kite is only spawned where entities tick, and a dimension without a sky gives nothing. Rain and snow both count as precipitation. Perk per tier: tier 1 is the plain kite (needs a free 3x3 way up, rain halves the output); tier 2 a waxed line (rain costs a quarter); tier 3 is rain-proof and ignores obstructions (blocks above it do not stop the kite, a crowded sky costs nothing; the altitude counts from the top of the winch, so the tether never runs through the winch itself); in a thunderstorm tiers 1-3 reel the kite in (no lightning is ever attracted), tier 4 flies at half output and a crowded sky costs it only half, tier 5 ignores thunder and a crowded sky. Tier 4 and 5 fly **two kites** on tethers from the same winch (one item, two entities, never a duplicate; a missing one is respawned): the first makes 60 % and the second 40 % of the base, so the pair makes at most base x length factor and one kite alone stays below it. Best case (full tether, a free sky, clear weather and the best gust) per tier: 16 / 150 / 1300 / 12999.75 / 128000 FE/t (base x factor 0.25 / 0.375 / 0.5 / 0.75 / 1 for tether 16 / 24 / 32 / 48 / 64); the buffer holds 600 ticks of it and a neighbour can take 5 times it per tick. A typical day on open ground (full tether, free sky, real gust curve, 75 % clear / 20 % rain / 5 % thunder) gives about 9.8 / 19.4 / 23.7 / 24.3 / 24.6 batteries of the same tier for tier 1-5 (tier 5 fills its battery in about 586 ticks at the very best). **Lightning:** in a thunderstorm only the kites that stay up (tier 4 and 5) can be struck, rarely (1 % per second and kite, then 5 minutes of cooldown): a purely visual bolt (no fire, no damage) gives the block 1 000 000 FE (config `kite_lightning_energy`), into its buffer first and, what does not fit, as a one-off burst into the neighbours that take FE (ignoring the per-tick push limit); the rest is lost. Config key of the base: `generators.kite_generator_tier_<n>.base`. An empty hand shows altitude, output and what limits it (storm, blocked, weather, crowded sky); a comparator reads the output as 0-15 of the peak, and the item tooltip names tether and perk. The kite entity is not saved: the block remembers the kite and spawns it again when its chunk loads, so a restart never leaves a stray kite. FE leaves the block into the neighbours on every side but the top and cannot be inserted.
  - **Power drain:** creative-only test block that takes any FE offered to it and pulls FE out of every neighbour (within that neighbour's limit), and destroys it.
  - **Photovoltaic cell:** the item both panels are made of: two quartz, a glass pane and redstone grains give two cells.
- **Storage:** blocks that buffer energy, so production and consumption do not have to run at the same time.
- **Machines:** energy-consuming machines that process materials from Resources.

### Status

Resources: compressed blocks are implemented, the rest is planned. Energy has batteries, solar and lunar panels, flow, vibration, thermoelectric and melt generators, mob wheels and kite generators; more generators are planned. JEI, Jade and REI support lives in standalone compat mods (see below).

## Commands

```
./gradlew build                 # build and check everything
./gradlew :dev:runClient        # all mods incl. JEI + Jade compat; -Pdev.rei swaps JEI for REI; -Pdev.mods=dotcraftresources,dotcraftenergy limits the content mods only, -Pdev.noCompat drops the compat mods (PowerShell splits `-Pdev.x` at the dot: quote it, e.g. `"./gradlew" ":dev:runClient" "-Pdev.rei"`)
./gradlew :mods:energy:runClient  # a single mod (same run/)
./gradlew newMod --id=magic --name="Magic" --depends=resources
./gradlew generateData  # regenerate src/generated/resources of every mod (or :mods:<mod>:generateData) (models, lang, recipes, loot, tags); CI fails if it is stale
./gradlew installGitHooks       # enable the commit-msg hook (also runs on IDE sync)
```

## Configuration

`config/dotcraft/resources/common.toml` (applies after a restart or `/reload`):

- `[compressed]` `enabled` (default true): switch for compressing; when off, all compress recipes are disabled and compressed blocks are hidden in recipe viewers.
- `[compressed]` `maxCompressLevel` (1-9, default 9): highest craftable compression level. Recipes that output a higher level are disabled; the decompress recipe of the first disabled level stays so existing blocks are not stuck.
- `[materials]` `enabled` (default true): master switch of all materials and their recipes; it overrides the switches below.
- `[materials.<material>]` `enabled`: one material. `[materials.<material>.<configKey>]` `enabled`: one form of it (the config key of the form, e.g. `ingots`, `gears`); the tool forms (`hammers`, `sandpapers`) are not here but in the startup config below; when off, its recipes (also alloy recipes that use it) are dropped and it is hidden, and a switched-off ore form stops the ore generation. Items and blocks that already exist stay.
- `[materials.<material>.ores.<entry>]` `enabled`: one ore generation entry of a material (`ore_<index>` where the entry has no id). `[materials.<alloy>.recipes.<alternative>]` `enabled`: the recipes of an alloy that use one alternative input.
- `[worldgen]` `enabled`: generation of material ores in new chunks.

`config/dotcraft/resources/startup.toml` (a startup config, read before anything is registered; needs a restart): `[materials.<material>.<configKey>]` `enabled` for each tool form a material has (`hammers`, `sandpapers`). A tool form that is switched off is not registered at all: no item, so it is not in the game, its tags or recipes. Because the registries then differ, a client and a server with different values may fail to connect; keep the file equal on both. The master switch `[materials] enabled` and `[materials.<material>] enabled` of the common config still drop the recipes and hide the tool items, but cannot unregister them.

## Materials and forms

Materials and forms are data, not code: `mods/resources/src/main/resources/materials/<id>.json` and `forms/<id>.json`. Items, blocks, models, tags, translations, loot, world generation, config switches and recipes are generated from them (`./gradlew generateData`, the output in `mods/resources/src/generated` is committed and must not be edited by hand). A new material is one file; a new form is one file plus a grayscale model and texture template.

### Forms

A form such as `ingot` or `gear` says how every item or block of that form is registered and crafted.

```json
{
  "id": "gear",
  "kind": "item",
  "order": 100,
  "configKey": "gears",
  "tag": { "parent": "gears" },
  "name": { "en_us": "%s Gear", "de_de": "%s-Zahnrad" },
  "external": "default",
  "conversions": [ { "kind": "shaped", "ratio": 1, "pattern": ["NIN", "IPI", "NIN"], "key": { "N": "nugget", "I": "ingot", "P": "minecraft:planks" } } ]
}
```

- Required: `id` (lower case, also the file name), `kind` (`item` or `block`), `order` (position in the creative tab and in every list of forms, unique), `configKey` (plural name in the config, unique, not `enabled` or `recipes`), `tag` (`parent` is the folder of the `c:` convention tag, `name` the optional name pattern inside it, e.g. `raw_%s`) and `name` (one pattern per supported locale, the material name is `%s`). A block also needs `block`.
- Optional: `template` (model and texture template, default the id), `tinted` (default true, the template is colored with the material color), `variants` (default 1; with more the templates are `<template>_<n>` and a material takes the one its id hashes to, so not all nuggets look alike), `path` (registry path pattern, default `%s_<id>`), `mined` (only found in the ground, so alloys lack it), `icon` (the form shown as the creative tab icon, the first one a material has wins), `requires` (forms a material needs to have this one), `tool` and `conversions`.
- `block` has `mapColor`, `sound`, `requiresTool`, `hardness`, `resistance`, `tool`, `tierGated` (the tool needs the tier of the material), `loot` (`self` or `ore`; `ore` needs `drop`, the form that is dropped as vanilla ores do, and `hosted`) and `hosted`, the name parts by richness and host for a form with one block per host and richness, such as the ore.
- `external` says what a material of an external metal (see below) does with the form: `excluded` (the external metal provides it, the material must not have it), `default` (the material has it unless it lists its forms) or `optional` (it may list the form). The default is `optional`.
- `tool` (`{ "durability": 1.0 }`) makes the form a crafting tool such as the hammer: a damageable item that stays in the crafting grid and loses one durability per craft. The durability is a factor of the vanilla tools of the tier of the material unless the material gives its own. A tool form is never a default form: only materials that list it have it, and each needs a `tools` entry.
- `neoforge:conditions` (standard NeoForge conditions `always`, `never`, `not`, `and`, `or`, `mod_loaded`) keep the form out of the game while a condition fails: it is not registered, not in the config and has no recipes, and so is every form that `requires` it or has it as the `drop` of its block.

### Conversions

A conversion says how a form is made of another form of the same material (`from`); the recipe is made wherever the material has both. Common options: `ratio`, `xp` (of a smelt or blast), `materials` (restrict the conversion to these material ids, all where empty), `external` and `externalRequired`.

| kind | meaning |
| --- | --- |
| `pack` | `ratio` (4 or 9) items of `from` make one of this form, and one makes `ratio` back (ingot and block, nugget and ingot). Where the big form is an external item (redstone grains and vanilla redstone) it is made shapeless, in both directions, for a material that has the small form and whose source names a tag and an item for the big form. |
| `smelt` | one item is smelted and blasted into one of this form. |
| `blast` | like `smelt`, but only in the blast furnace (blocks of raw material and dust). |
| `craft` | `ratio` (1 to 9) items of `from` make one, shapeless and one way. |
| `crush` | one item of `from` plus a hammer make `ratio` items of this form; the hammer stays and wears. |
| `forge` | a shaped recipe: a hammer over a row of `ratio` (1 to 3) items of `from` makes one item (three ingots make a plate, one ingot makes a dust). |
| `shaped` | a shaped recipe from `pattern` (rows) and `key` (symbol to a form id, which stands for the tag of that form for the material, or an item tag such as `minecraft:planks`); makes `ratio` (1 to 9) items. A key value `@hammer` is the hammer tag of the tier of the material (the hammer stays and wears, the recipe is dropped where no hammer of that tier is enabled). Has no `from`. |
| `shapeless` | the same shapeless: `pattern` is one row whose symbols are the ingredients, a repeated symbol is used that often; makes `ratio` (1 to 9) items. Has no `from`. |
| `alloy` | alloys are mixed in this form: the same form of every input of the alloy makes the alloy form (see `alloy` below). Has no `from`; `external` is the key of the external item of an input that lacks the form. |

In `shaped` and `shapeless`, the symbol `#` is the ingredient of the material for a tool form (its `tools` entry). The recipe is skipped where a form of the key is missing, and where a tool form is used by a material without a `tools` entry. A material that uses an external metal resolves a missing form through the tag its `external` source names for it, so the dust of vanilla iron is crushed from the vanilla raw iron (`c:raw_materials/iron`) and the plate is forged from the vanilla ingot. `"externalRequired": true` on a `smelt` or `blast` means: for an external material that has `from` but not this form, the source must name the external item of this form, and that item is the result (grains smelt to the vanilla nugget).

**Rods and wires:** every metal material (all with an ingot, also the external metals and the alloys) has the forms `rod` (item `<material>_rod`, tag `c:rods/<material>`) and `wire` (`<material>_wire`, `c:wires/<material>`; both are also in `c:rods` and `c:wires`, switches `rods` and `wires`). Two ingots over a hammer of the tier of the material (a column) make 3 rods, and a hammer, a sandpaper (`c:tools/sandpaper`) and a rod make 2 wires; hammer and sandpaper stay in the grid and wear. The wooden rod (`c:rods/wooden`) is untouched.

### Hammers

Crush and forge need a hammer. A hammer has to be at least as good as the material it works, so a recipe takes the tag `dotcraftresources:hammers/<tier>`, generated per tier that has a hammer (stone, iron, diamond, netherite; steel counts as iron). Such a tag holds every hammer of that tier or a better one: a diamond material needs a diamond or netherite hammer, a stone hammer works stone-tier materials. For materials below the lowest hammer tier (`none`, `wood`, `gold`) the tag is `c:tools/hammer`, which holds every hammer. Every crush and forge recipe also carries the config condition `{"type": "dotcraftresources:config", "switch": "tool", "form": "hammer", "tier": "<tier>"}`, which holds while some material of that tier or better has its hammer form switched on, so the recipe is dropped when no usable hammer is left. The tags are static: a hammer that is switched off in the startup config is not registered, its entry in the tags is optional (`required: false`) and is skipped, so the data loads without errors; a hammer that is only hidden by the common master or material switch still counts as an ingredient while another enabled hammer keeps the recipe loaded.

### Materials

```json
{
  "id": "tin",
  "color": "#C9D3D6",
  "name": { "en_us": "Tin", "de_de": "Zinn" },
  "tier": "stone",
  "forms": ["dust", "ore"],
  "ore": [ { "hosts": ["stone", "deepslate"], "richness": ["poor"], "veinSize": 7, "count": 6, "minY": -16, "maxY": 80 } ]
}
```

- Required: `id`, `color` (`#RRGGBB`), `name` for every supported locale and `tier` (`none wood stone copper gold iron diamond netherite`; the lowest tool tier that gets a drop from the metal and ore blocks, `none` for no gating).
- `forms`: the forms the material has. Without it a material has all forms except tool forms (and, for an alloy, except `mined` forms such as raw and ore).
- `ore`: the world generation entries, only with a hosted form such as `ore`; without it the one default entry (`ore_0`, veinSize 8, count 12, height -16 to 112, all default hosts) applies. Each entry has an optional `id` (default `ore_<index>`), `hosts` (`stone deepslate nether end blackstone basalt`, default stone, deepslate, nether, end), `richness` (`poor normal rich`, default all; poor ore drops 0 or 1 raw item, normal 1, rich 2), `veinSize` (1-64), `count` (veins per chunk, 1-256), `minY`, `maxY` (-64 to 320, clamped to the dimension), `biomeTag`, `dimensions` (`overworld nether end`, default those of the hosts) and `exposure` (`air lava water`, one value or a list: a block of a vein is only placed if a neighbour is one of them). Poor ore is generated 1.5 times as often over a wider range, rich ore a quarter as often. One block is registered per host and richness; every entry has its own config switch.
- `alloy`: `{ "inputs": [ { "material": "copper", "count": 3 }, { "material": ["coal", "charcoal"] } ], "result": 4 }`; at least 2 inputs, each `count` 1-9 (default 1), a list of materials is a set of alternatives, `result` (1-9) is the number of items one mix makes, the sum of the counts where absent. One shapeless recipe mixes the same form of every input in each form that has an `alloy` conversion (dust and grains); the mixed dust is smelted to the ingot. An input that lacks the form is resolved through its `external` source (the key named by the `alloy` conversion). No more than nine items in all. An alloy has no `external` and no mined forms.
- `external`: a list of sources for a material that adds parts to a metal provided from outside, such as iron, copper or gold of vanilla: `[{ "mod": "othermod", "forms": { "ingot": { "tag": "c:ingots/iron", "item": "minecraft:iron_ingot" } } }]`. A source maps a form id to the item tag that stands for the form and, optionally, the concrete item a recipe makes of it. `mod` makes the recipes that use the source load only while that mod is loaded (the item need not exist at generation time); the first source has no `mod`, each mod appears once. Such a material registers none of the forms the external metal provides (`external: excluded`) and has the `default` ones unless it lists its forms. A material with an `external` list may still have `ore`: then only the poor and rich ore blocks are registered (iron, copper, gold, coal, lapis, redstone), their raw drop is the vanilla raw item and normal ore stays vanilla. Netherite has an own `nugget` because the form is `optional` for external materials.
- `smelt` (default true): `false` turns off all smelt and blast conversions of the material (coal, whose dust is converted by hand-written recipes).
- `tools`: per tool form the `ingredient` (item tag of the `#` in its recipe, e.g. `c:storage_blocks/iron`), optional `count` (1-9, items a shapeless recipe takes) and `durability` (absolute, for tiers without a fitting vanilla value such as steel).
- `neoforge:conditions`: as for forms; a material left out has no items, blocks, config switches or recipes, and every file it or its forms generate carries the conditions.

### Recipe ids

All ids are `dotcraftresources:material/<material>/<name>`, where `<name>` is

- `<to>_from_<from>` for a pack (both directions) and a craft;
- `<to>_from_<from>_smelting` and `<to>_from_<from>_blasting` for a smelt (a blast has only the second);
- `<to>_from_<from>_crushing` for a crush and `<to>_from_<from>_forging` for a forge;
- `<to>` for a shaped or shapeless conversion, e.g. `iron/gear`, `iron/rod`, `iron/wire`, `iron/hammer`, `iron/sandpaper`;
- `<to>_from_alloy` for an alloy, followed for each input by `_<material>` where it has alternatives, e.g. `steel/ingot_from_alloy_charcoal`.

A source of another mod appends `_<mod>` at the end of the id (`..._crushing_<mod>`).

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

JEI, Jade and REI support are standalone mods, one per third-party mod, each covering all .Craft mods: `mods/jei` (`dotcraftjei`, `.Craft: JEI`), `mods/jade` (`dotcraftjade`, `.Craft: Jade`), `mods/rei` (`dotcraftrei`, `.Craft: REI`), under `net.samtrion.dotcraft.<jei|jade|rei>`. The content mods (resources, energy, ...) know nothing about them: no code, no Gradle dependency, no `neoforge.mods.toml` entry. The compat mods depend optionally on the content mods and only touch a content mod's classes when it is loaded. Each compat mod hard-requires its third-party mod (`type="required"`: JEI, Jade, REI), so install a compat mod only together with that mod; the content mods work without any of them. The accepted version range of the third-party mod is the `compat_version_range` property in the compat mod's `gradle.properties`.

To hook a new .Craft mod into the compat mods, add a `<mod>` subpackage with a plugin class to each of `mods/jei`, `mods/jade`, `mods/rei` (see the `energy` and `resources` packages), add the new mod as `compileOnly` in their `build.gradle` and as an optional dependency in their `neoforge.mods.toml`, and guard the plugin with `ModList.get().isLoaded(...)`, keeping the access to the mod's classes in a separate method.

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
