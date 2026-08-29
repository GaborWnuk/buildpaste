# BuildPaste

Paste builds from [buildpaste.net](https://buildpaste.net) straight into your world.
Select two corners, upload what you built, and paste any build back in — turned to
face whichever way you are looking.

## About this port

This is an unofficial **NeoForge** port of
[BuildPaste](https://legacy.curseforge.com/minecraft/bukkit-plugins/buildpaste), a Bukkit
server plugin by MistrX.

The reason is simple: the original only exists as a Bukkit/Spigot plugin, and I don't run
Bukkit. My server and my client are NeoForge, so the plugin could not be installed on
either — a Bukkit plugin and a NeoForge mod share no code at all. This port keeps the
behaviour and speaks the same buildpaste.net protocol, so builds uploaded by the original
plugin paste correctly here and builds uploaded here work anywhere else BuildPaste runs.

It is dedicated to my son, who would rather spend an afternoon building a castle than
fighting anything that lives in one :)

## Usage

Select a region, look at the front of your build, and upload it:

```
/pos1                    set the first corner (or left-click with the selector)
/pos2                    set the second corner (or right-click with the selector)
/selector                get the position selector stick
/removepos               clear both corners
/upload [name]           upload the selected region
```

Paste builds back in, oriented to the way you are facing:

```
/paste                          paste the build selected on buildpaste.net
/paste <build-id>               paste a particular build
/paste <build-id> dontplaceair  drop the build into the scene, keeping what is already there
/undopaste                      put back whatever the last paste overwrote
```

Build from your own inventory instead of pasting outright:

```
/construct [build-id]    list the materials the build needs and what you are missing
/construct confirm       build as much of it as your inventory pays for
```

Share and select builds:

```
/setbuild <build-id>     make a build your selected one
/sharebuild [build-id]   post a clickable paste link to everyone on the server
/pastepaper [name]       get a piece of paper that pastes your selected build on right-click
/connectaccounts [email] link your Minecraft account to your buildpaste.net account
```

Operator settings:

```
/buildpaste                                          what this is, with example links
/buildpaste allowall | disallowall                   let everyone use it, or not
/buildpaste pastePermission <player> allow|disallow  grant one player one permission
/buildpaste uploadPermission <player> allow|disallow
/buildpaste constructPermission <player> allow|disallow
/buildpaste allPermissions <player> allow|disallow
/buildpaste debug true|false                         trace pastes in the server log
```

Operators may do everything by default. Anyone else needs a grant, and grants last until
the server restarts.

## Installing

The mod is **server-side**. It registers no blocks, items, entities or network channels,
so:

- On a **server**, it is the only place it needs to be installed. Players can join with an
  unmodded client and still use every command — the commands and the chat buttons come
  from the server, and the selector and paste paper are an ordinary stick and an ordinary
  piece of paper with a name on them.
- On a **client**, install it to use the commands in your own singleplayer worlds.

Kotlin for Forge must be installed alongside it, wherever it runs.

## Supported versions

| Minecraft       | Loader   | Where                                                       |
| --------------- | -------- | ----------------------------------------------------------- |
| 26.1.2 or newer | NeoForge | this repository — tested on 26.1.2                          |
| 1.16 – 1.21.10  | Bukkit   | [the original plugin](https://legacy.curseforge.com/minecraft/bukkit-plugins/buildpaste) |

Required companion mod: **Kotlin for Forge** 6.3.0 or newer.

The mod declares a `[26.1.2,26.2)` version range, so the loader refuses to load it on an
unsupported version rather than crashing. Support for a newer Minecraft release is added
by one `match(...)` line in [settings.gradle.kts](settings.gradle.kts).

## What changed from the plugin

The port is a rewrite against a different platform — Bukkit and NeoForge share no API —
but it keeps the commands, the chat, and the wire protocol as they were. Along the way it
fixes a handful of defects in the original:

- **Requests no longer block the server.** Every backend call the plugin made ran
  synchronously inside the command handler, freezing the server for the length of the
  round trip. Calls are asynchronous here and resume on the server thread.
- **Uploading unusual blocks works.** The plugin called `equals` on a null block id, so
  uploading any block outside its 1140-name table threw and lost the whole upload.
  Untabled blocks now travel as their name.
- **Chests and signs keep their contents.** The protocol has always had a field for
  block-entity data, but the plugin sent it empty and ignored it on the way back, so
  every build it uploaded pasted hollow. It is captured and restored now.
- **Undo restores what was there.** Undo re-applied the paste rotation to the blocks it
  had captured, so restored stairs and logs came back facing the wrong way.
- **Pasting over a build corrects it.** Placement compared only the block type, so a
  stair already in the right spot but facing the wrong way was left alone.
- **Debug output goes to the log**, rather than being broadcast to every player on the
  server as chat.

## Building from source

Requirements: Java 25, which Gradle provisions automatically thanks to the Foojay
toolchain resolver.

```sh
./gradlew build
```

The jar lands in `versions/26.1.2-neoforge/build/libs/`.

To run a development client or server:

```sh
./gradlew :26.1.2-neoforge:runClient
./gradlew :26.1.2-neoforge:runServer
```

## Credits

Original BuildPaste plugin and the buildpaste.net service by **MistrX** — thank you. This
port is not affiliated with or endorsed by them.

The port is licensed under the [MIT license](LICENSE).
