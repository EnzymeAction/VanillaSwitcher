package net.vanillaswitcher;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.vanillaswitcher.config.ModConfig;
import net.vanillaswitcher.gui.ConfigScreen;
import net.vanillaswitcher.hud.AlertToast;
import net.vanillaswitcher.hud.ArmorHud;
import net.vanillaswitcher.lang.Text;
import net.vanillaswitcher.util.BrightnessTransition;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLScancode;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class VanillaSwitcherClient implements ClientModInitializer {
    public static final String MOD_ID = "vanillaswitcher";

    public enum BindTarget {
        ACTIVATE,

        MENU
    }

    private static ModConfig config;
    private static boolean active = false;
    private static boolean activateWasDown = false;
    private static boolean menuWasDown = false;

    private static BindTarget listeningFor = null;

    private static Object prevParticles = null;
    private static int prevRenderDistance = -1;

    private static final BrightnessTransition brightness = new BrightnessTransition();

    public static ModConfig getConfig() {
        return config;
    }

    public static boolean isActive() {
        return active;
    }

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(VanillaSwitcherClient::onClientTick);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud"),
                VanillaSwitcherClient::renderArmorHud
        );

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "alert_toast"),
                VanillaSwitcherClient::renderToast
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("vanillaswitcher")
                        .executes(ctx -> {
                            openConfigScreen();
                            return 1;
                        })));
    }

    private static void openConfigScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new ConfigScreen(mc.gui.screen())));
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player == null) {
            activateWasDown = false;
            menuWasDown = false;
            return;
        }

        pollBinds(mc);
    }

    private static void pollBinds(Minecraft mc) {
        boolean activateDown = isBindDown(config.bindKeyCode);
        boolean menuDown = isBindDown(config.menuBindKeyCode);

        if (mc.gui.screen() == null && listeningFor == null) {
            if (activateDown && !activateWasDown) {
                toggle();
            }
            if (menuDown && !menuWasDown) {
                openConfigScreen();
            }
        }

        activateWasDown = activateDown;
        menuWasDown = menuDown;
    }

    private static boolean isBindDown(int keyCode) {
        return keyCode != -1 && InputConstants.isKeyDown(keyCode);
    }

    public static void setListeningFor(BindTarget target) {
        listeningFor = target;
    }

    public static BindTarget getListeningFor() {
        return listeningFor;
    }

    public static boolean isListeningForBind() {
        return listeningFor != null;
    }

    public static void captureBindKey(int keyCode) {
        BindTarget target = listeningFor;
        listeningFor = null;
        if (target == null) {
            return;
        }

        if (target == BindTarget.ACTIVATE) {
            config.bindKeyCode = keyCode;
            if (keyCode != -1 && config.menuBindKeyCode == keyCode) {
                config.menuBindKeyCode = -1;
            }
        } else {
            config.menuBindKeyCode = keyCode;
            if (keyCode != -1 && config.bindKeyCode == keyCode) {
                config.bindKeyCode = -1;
            }
        }
        config.save();
    }

    public static String bindDisplayName(BindTarget target) {
        return keyName(target == BindTarget.ACTIVATE ? config.bindKeyCode : config.menuBindKeyCode);
    }

    private static String keyName(int keyCode) {
        if (keyCode == -1) {
            return Text.NOT_BOUND.in(config.language);
        }
        if (keyCode >= SDLScancode.SDL_SCANCODE_F1 && keyCode <= SDLScancode.SDL_SCANCODE_F12) {
            return "F" + (keyCode - SDLScancode.SDL_SCANCODE_F1 + 1);
        }
        if (keyCode >= SDLScancode.SDL_SCANCODE_F13 && keyCode <= SDLScancode.SDL_SCANCODE_F24) {
            return "F" + (keyCode - SDLScancode.SDL_SCANCODE_F13 + 13);
        }
        String special = switch (keyCode) {
            case SDLScancode.SDL_SCANCODE_LSHIFT -> "LEFT SHIFT";
            case SDLScancode.SDL_SCANCODE_RSHIFT -> "RIGHT SHIFT";
            case SDLScancode.SDL_SCANCODE_LCTRL -> "LEFT CTRL";
            case SDLScancode.SDL_SCANCODE_RCTRL -> "RIGHT CTRL";
            case SDLScancode.SDL_SCANCODE_LALT -> "LEFT ALT";
            case SDLScancode.SDL_SCANCODE_RALT -> "RIGHT ALT";
            case SDLScancode.SDL_SCANCODE_SPACE -> "SPACE";
            case SDLScancode.SDL_SCANCODE_TAB -> "TAB";
            case SDLScancode.SDL_SCANCODE_RETURN -> "ENTER";
            case SDLScancode.SDL_SCANCODE_BACKSPACE -> "BACKSPACE";
            case SDLScancode.SDL_SCANCODE_CAPSLOCK -> "CAPS LOCK";
            case SDLScancode.SDL_SCANCODE_INSERT -> "INSERT";
            case SDLScancode.SDL_SCANCODE_DELETE -> "DELETE";
            case SDLScancode.SDL_SCANCODE_HOME -> "HOME";
            case SDLScancode.SDL_SCANCODE_END -> "END";
            case SDLScancode.SDL_SCANCODE_PAGEUP -> "PAGE UP";
            case SDLScancode.SDL_SCANCODE_PAGEDOWN -> "PAGE DOWN";
            case SDLScancode.SDL_SCANCODE_UP -> "UP";
            case SDLScancode.SDL_SCANCODE_DOWN -> "DOWN";
            case SDLScancode.SDL_SCANCODE_LEFT -> "LEFT";
            case SDLScancode.SDL_SCANCODE_RIGHT -> "RIGHT";
            default -> null;
        };
        if (special != null) {
            return special;
        }
        String name = SDLKeyboard.SDL_GetScancodeName(keyCode);
        if (name != null && !name.isEmpty()) {
            return name.toUpperCase();
        }
        return "KEY " + keyCode;
    }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        active = !active;

        if (active) {
            applyCrystalMode(mc);
            if (config.showAlertToast) {
                AlertToast.show("&fSwitched to &dCrystal &fmode.");
            }
        } else {
            restoreVanillaMode(mc);
            if (config.showAlertToast) {
                AlertToast.show("&fSwitched to &aVanilla &fmode.");
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setOptionValue(OptionInstance<?> option, Object value) {
        ((OptionInstance) option).set(value);
    }

    private static Object findParticleStatus(String constantName) {
        Object current = Minecraft.getInstance().options.particles().get();
        if (!(current instanceof Enum<?> currentEnum)) {
            return null;
        }

        for (Object constant : currentEnum.getDeclaringClass().getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(constantName)) {
                return constant;
            }
        }
        return null;
    }

    private static void renderArmorHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        if (config != null && active && config.showArmorHud) {
            ArmorHud.render(graphics, mc.font, mc, graphics.guiWidth(), graphics.guiHeight());
        }
    }

    private static void renderToast(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        if (config != null && config.showAlertToast) {
            AlertToast.render(graphics, mc.font, graphics.guiWidth(), graphics.guiHeight());
        }
    }

    private static void applyCrystalMode(Minecraft mc) {
        OptionInstance<?> particles = mc.options.particles();
        if (config.disableParticles) {
            prevParticles = particles.get();
            Object minimal = findParticleStatus("MINIMAL");
            if (minimal != null) {
                setOptionValue(particles, minimal);
            }
        }

        if (config.switchRenderDistance) {
            prevRenderDistance = mc.options.renderDistance().get();
            mc.options.renderDistance().set(config.crystalRenderDistance);
        }

        if (config.smoothLightingSwitch) {
            mc.options.ambientOcclusion().set(false);
        }

        fadeBrightness(true);
    }

    private static void restoreVanillaMode(Minecraft mc) {
        if (config.disableParticles && prevParticles != null) {
            setOptionValue(mc.options.particles(), prevParticles);
            prevParticles = null;
        }

        if (config.switchRenderDistance) {
            mc.options.renderDistance().set(config.vanillaRenderDistance);
        }

        if (config.smoothLightingSwitch) {
            mc.options.ambientOcclusion().set(true);
        }

        fadeBrightness(false);
    }

    private static void fadeBrightness(boolean crystalOn) {
        if (!config.smoothBrightness) {
            brightness.snapTo(BrightnessTransition.VANILLA);
            return;
        }

        int strength = Math.max(1, Math.min(15, config.crystalBrightness));
        double target = crystalOn ? strength : BrightnessTransition.VANILLA;
        brightness.fadeTo(target, strength, config.brightnessTransitionSeconds, System.nanoTime());
    }

    public static float brightnessMultiplier() {
        if (config == null || !config.smoothBrightness) {
            return 1.0f;
        }
        double value = brightness.valueAt(System.nanoTime());
        return (float) Math.max(1.0, Math.min(15.0, value));
    }

    public static boolean shouldDisableFog() {
        return active && config.noFog;
    }

    public static boolean shouldResizeTotem() {
        return active && config.resizeTotem;
    }

    public static float totemPopScale() {
        return config.totemPopScale;
    }

    public static float totemHandScale() {
        return config.totemHandScale;
    }

    public static boolean shouldDisableParticles() {
        return active && config.disableParticles;
    }

    public static float fireOffset() {
        return (active && config.applyOffsets) ? config.fireOffset : 0f;
    }

    public static float shieldOffset() {
        return (active && config.applyOffsets) ? config.shieldOffset : 0f;
    }
}
