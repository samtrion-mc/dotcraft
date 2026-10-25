# .Craft

Monorepo of NeoForge mods for Minecraft 26.1.2 (NeoForge 26.1.2.124, Java 25).

## Layout

- `mods/<name>/` - one content mod per directory (`:mods:<name>`, mod id `dotcraft_<name>`, package `net.samtrion.dotcraft.<name>`). Metadata lives in the mod's `gradle.properties` (`mod_id`, `mod_name`, `mod_description`).
- `dev/` - runs all mods together in one game (`run/`).
- `build-logic/` - convention plugins (`dotcraft.neoforge-base`, `dotcraft.neoforge-mod`, `dotcraft.root`).
- `.githooks/` - commit message check.

All mods share one version, derived from git tags by git-semver.

## Commands

```
./gradlew build                 # build and check everything
./gradlew :dev:runClient        # all mods together (-Pdev.mods=resources,energy to limit)
./gradlew :mods:energy:runClient  # a single mod (run/energy/)
./gradlew newMod --id=magic --name="Magic" --depends=resources [--no-compat]
./gradlew installGitHooks       # enable the commit-msg hook (also runs on IDE sync)
```

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
