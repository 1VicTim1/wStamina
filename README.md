# wStamina

Stamina plugin for Paper `1.21.8` with optional integrations:
- LuckPerms
- WorldGuard
- PlaceholderAPI
- BetterHud

## Features

- Drains stamina while a player is sprinting.
- Forces sprint off when stamina reaches `0`.
- Starts a regen delay after sprint activity ends (`regen-delay-ticks`, default `40` ticks = 2 seconds).
- Repeated sprint attempts with zero stamina keep sprint blocked and keep regen delayed.
- Regeneration during movement is configurable (`stamina.regen-while-walking`).
- Base stamina is configurable in `config.yml`; final max stamina is scaled by LuckPerms multiplier.

## Command

- `/wstamina` shows your current stamina.
- `/wstamina reload` reloads `config.yml` and `lang.yml`.

## Integrations

### LuckPerms

Supported permissions:
- `wstamina.nodrain` disables stamina drain.
- `wstamina.multiplier.<value>` sets stamina max multiplier.
  - Example: `wstamina.multiplier.1.5`

Registered LP contexts:
- `stamina_state=normal|exhausted`
- `stamina_region=normal|no_drain|force_zero`
- `stamina_drain=active|blocked`
- `stamina_multiplier=<current value>`

Context keys are configurable in `config.yml` under `luckperms.contexts.*`.

### WorldGuard

Registered flags:
- `wstamina-no-drain` disables drain in a region.
- `wstamina-force-zero` forces stamina to `0` in a region.

Flag names are configurable in `config.yml` under `worldguard.flags.*`.
If flag names are changed after startup, a full server restart is required.

### PlaceholderAPI

Default identifier: `wstamina` (`placeholders.papi.identifier`).

Registered placeholders:
- `%wstamina_current%`
- `%wstamina_max%`
- `%wstamina_percent%`
- `%wstamina_state%`
- `%wstamina_region%`
- `%wstamina_multiplier%`
- `%wstamina_nodrain%`
- `%wstamina_exhausted%`

### BetterHud

Default namespace: `wstamina` (`placeholders.betterhud.namespace`).

Registered IDs:
- `wstamina_current`
- `wstamina_max`
- `wstamina_percent`
- `wstamina_state`
- `wstamina_region`
- `wstamina_multiplier`
- `wstamina_nodrain`
- `wstamina_exhausted`

Use these in BetterHud with its standard placeholder syntax.

## Configuration files

- `config.yml` contains stamina logic, integration toggles, and debug modules.
- `lang.yml` contains prefix and all user-facing plugin messages.

## Build

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

## Versions in gradle.properties

`gradle.properties` stores:
- plugin version (`pluginVersion`)
- dependency versions (compileOnly artifacts)
- Java version (`javaVersion`)
- run server Minecraft version (`minecraftRunVersion`)
