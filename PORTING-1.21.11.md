# Porting notes: 26.2 -> 1.21.11

Features, config format and settings screen are unchanged. Only the code that talks to Minecraft
changed, because 1.21.11 is the last *obfuscated* release (Java 21) and 26.x is the unobfuscated line
(Java 25) with a reworked GUI/render API. Sources use Mojang's official names
(`loom.officialMojangMappings()`), which is also what 26.x uses.

## What changed

| Area | 26.2 | 1.21.11 |
| --- | --- | --- |
| Build | `fabric-loom`, Java 25, no mappings | `fabric-loom-remap`, Java 21, Mojang mappings, `modImplementation` |
| GUI graphics | `GuiGraphicsExtractor`: `text`, `centeredText`, `item`, `itemDecorations` | `GuiGraphics`: `drawString`, `drawCenteredString`, `renderItem`, `renderItemDecorations` |
| Screens/widgets | `extractRenderState`, list entry `extractContent` | `render`, list entry `renderContent` |
| Client state | `mc.gui.screen()`, `mc.gui.setScreen()`, `mc.gui.hud.isHidden()` | `mc.screen`, `mc.setScreen()`, `mc.options.hideGui` |
| Fabric API | `ClientCommands.literal` | `ClientCommandManager.literal` |
| HUD accessor mixin | `Hud` | `Gui` (same `HOTBAR_SPRITE` field) |
| Fullbright mixin | `Lightmap#render`, 5th `Std140Builder.putFloat` | `LightTexture#updateLightTexture`, 2nd `Double.floatValue()` (gamma) |
| Fog mixin | `setupFog` returns `FogData`, `@Local(name=...)` | `setupFog` returns `Vector4f`, positional `LocalCapture.CAPTURE_FAILHARD` |
| Fire mixin | `submitFire(..., SubmitNodeCollector, ...)` at HEAD | static `renderFire(..., MultiBufferSource, ...)` after `pushPose()` |
| Unchanged | particles, held item (shield/totem in hand), totem pop size, totem pop position | |

## How it was checked

Every class, method, field and descriptor above was checked against 1.21.11 data, not guessed:
Mojang-mapped 1.21.11 source of uku3lig/armor-hud, Fabric API (`1.21.11` branch) and NeoForge
(`1.21.11` branch); BactroMod's last 1.21.11 commit (fog, lightmap, fire); Totem Tweaks and
ParticleCulling `1.21.11` branches; and the mappings.dev 1.21.11 tables.

Also run: the Minecraft-independent classes (`BrightnessTransition`, `Language`, `Text`) compile on
`--release 21` and pass behaviour checks; `javac` over the full tree reports only missing-dependency
errors (no syntax or structural errors); mixin config and entrypoint classes all exist.

## NOT verified

**This has never been compiled against the real 1.21.11 jars or launched.** Fabric's Maven and
Mojang's servers were not reachable where the port was made. First thing to do: `gradle build`
(or push and let `.github/workflows/build.yml` build it).

If something misbehaves in game, the riskiest two spots are:

1. `MixinFogRenderer`: positional local capture. If the local layout differs, that one mixin is
   skipped (config is `required: false`) and fog stays on; the log shows a Mixin warning.
2. `MixinLightTexture`: relies on gamma being the 2nd `Double.floatValue()` in
   `updateLightTexture`. If wrong, fullbright does nothing.

Quick in-game check: toggle the mode and confirm particles stop, fog is gone, the totem pop shrinks
and stays centred, fire/shield sit lower, armor HUD shows next to the hotbar, and brightness fades.
