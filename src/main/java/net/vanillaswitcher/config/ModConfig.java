package net.vanillaswitcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.vanillaswitcher.lang.Language;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path PATH = CONFIG_DIR.resolve("vanillaswitcher.json");

    public static final float TOTEM_SCALE_MIN = 0.10f;
    public static final float TOTEM_SCALE_MAX = 2.00f;
    public static final int BRIGHTNESS_TRANSITION_MIN_SECONDS = 1;
    public static final int BRIGHTNESS_TRANSITION_MAX_SECONDS = 5;

    public Language language = Language.ENGLISH;

    public int bindKeyCode = -1;

    public int menuBindKeyCode = -1;

    public boolean disableParticles = true;

    public boolean noFog = true;

    @SerializedName(value = "resizeTotem", alternate = {"resizeTotemPop"})
    public boolean resizeTotem = true;

    public float totemHandScale = 1.0f;

    public float totemPopScale = 0.5f;

    public boolean smoothBrightness = true;

    public int crystalBrightness = 3;

    public float brightnessTransitionSeconds = 3.0f;

    public boolean switchRenderDistance = true;
    public int crystalRenderDistance = 12;
    public int vanillaRenderDistance = 32;

    public boolean applyOffsets = true;
    public float fireOffset = -100f;
    public float shieldOffset = -20f;

    public boolean showArmorHud = true;
    public boolean showAlertToast = true;

    public boolean smoothLightingSwitch = true;

    public static ModConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("[Vanilla Switcher] Failed to read config, using defaults: " + e);
            }
        }
        ModConfig fresh = new ModConfig();
        fresh.save();
        return fresh;
    }

    private void sanitize() {
        if (language == null) {
            language = Language.ENGLISH;
        }
        crystalBrightness = Math.max(1, Math.min(15, crystalBrightness));
        brightnessTransitionSeconds = Math.max(BRIGHTNESS_TRANSITION_MIN_SECONDS,
                Math.min(BRIGHTNESS_TRANSITION_MAX_SECONDS, brightnessTransitionSeconds));
        totemPopScale = Math.max(TOTEM_SCALE_MIN, Math.min(TOTEM_SCALE_MAX, totemPopScale));
        totemHandScale = Math.max(TOTEM_SCALE_MIN, Math.min(TOTEM_SCALE_MAX, totemHandScale));
        crystalRenderDistance = Math.max(2, Math.min(32, crystalRenderDistance));
        vanillaRenderDistance = Math.max(2, Math.min(32, vanillaRenderDistance));
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[Vanilla Switcher] Failed to save config: " + e);
        }
    }
}
