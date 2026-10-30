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
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

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
    private static Boolean prevBobView = null;
    private static Double prevScreenEffectScale = null;
    private static Double prevFovEffectScale = null;
    private static Double prevDarknessEffectScale = null;
    private static Double prevDamageTiltStrength = null;

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
        mc.execute(() -> mc.setScreen(new ConfigScreen(mc.screen)));
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
        boolean activateDown = isBindDown(mc, config.bindKeyCode);
        boolean menuDown = isBindDown(mc, config.menuBindKeyCode);

        if (mc.screen == null && listeningFor == null) {
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

    private static boolean isBindDown(Minecraft mc, int keyCode) {
        return keyCode != -1 && InputConstants.isKeyDown(mc.getWindow(), keyCode);
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

    public static void captureBindKey(int glfwKeyCode) {
        BindTarget target = listeningFor;
        listeningFor = null;
        if (target == null) {
            return;
        }

        if (target == BindTarget.ACTIVATE) {
            config.bindKeyCode = glfwKeyCode;
            if (glfwKeyCode != -1 && config.menuBindKeyCode == glfwKeyCode) {
                config.menuBindKeyCode = -1;
            }
        } else {
            config.menuBindKeyCode = glfwKeyCode;
            if (glfwKeyCode != -1 && config.bindKeyCode == glfwKeyCode) {
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
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) {
            return name.toUpperCase();
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LEFT ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT ALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS LOCK";
            case GLFW.GLFW_KEY_INSERT -> "INSERT";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PAGE UP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PAGE DOWN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> "KEY " + keyCode;
        };
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

    private static void renderArmorHud(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        if (config != null && active && config.showArmorHud) {
            ArmorHud.render(graphics, mc.font, mc, graphics.guiWidth(), graphics.guiHeight());
        }
    }

    private static void renderToast(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
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

        applyAccessibility(mc);

        fadeBrightness(true);
    }

    private static void applyAccessibility(Minecraft mc) {
        if (config.disableViewBobbing) {
            prevBobView = mc.options.bobView().get();
            mc.options.bobView().set(false);
        }
        if (config.disableDistortionEffects) {
            prevScreenEffectScale = mc.options.screenEffectScale().get();
            mc.options.screenEffectScale().set(0.0);
        }
        if (config.disableFovEffects) {
            prevFovEffectScale = mc.options.fovEffectScale().get();
            mc.options.fovEffectScale().set(0.0);
        }
        if (config.disableDarknessPulsing) {
            prevDarknessEffectScale = mc.options.darknessEffectScale().get();
            mc.options.darknessEffectScale().set(0.0);
        }
        if (config.disableDamageTilt) {
            prevDamageTiltStrength = mc.options.damageTiltStrength().get();
            mc.options.damageTiltStrength().set(0.0);
        }
    }

    private static void restoreAccessibility(Minecraft mc) {
        if (prevBobView != null) {
            mc.options.bobView().set(prevBobView);
            prevBobView = null;
        }
        if (prevScreenEffectScale != null) {
            mc.options.screenEffectScale().set(prevScreenEffectScale);
            prevScreenEffectScale = null;
        }
        if (prevFovEffectScale != null) {
            mc.options.fovEffectScale().set(prevFovEffectScale);
            prevFovEffectScale = null;
        }
        if (prevDarknessEffectScale != null) {
            mc.options.darknessEffectScale().set(prevDarknessEffectScale);
            prevDarknessEffectScale = null;
        }
        if (prevDamageTiltStrength != null) {
            mc.options.damageTiltStrength().set(prevDamageTiltStrength);
            prevDamageTiltStrength = null;
        }
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

        restoreAccessibility(mc);

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
