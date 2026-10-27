package net.vanillaswitcher.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.vanillaswitcher.VanillaSwitcherClient;
import net.vanillaswitcher.VanillaSwitcherClient.BindTarget;
import net.vanillaswitcher.config.ModConfig;
import net.vanillaswitcher.lang.Language;
import net.vanillaswitcher.lang.Text;
import org.lwjgl.sdl.SDLScancode;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class ConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 32;

    private final Screen parent;
    private ConfigList list;
    private Button doneButton;

    private boolean rebuildPending = false;

    public ConfigScreen(Screen parent) {
        super(Component.literal(Text.TITLE.in(VanillaSwitcherClient.getConfig().language)));
        this.parent = parent;
    }

    private Language language() {
        return VanillaSwitcherClient.getConfig().language;
    }

    @Override
    protected void init() {
        list = new ConfigList(minecraft, width, height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT,
                () -> rebuildPending = true);
        this.addRenderableWidget(list);

        doneButton = Button.builder(
                Component.literal(Text.DONE.in(language())),
                button -> onClose()
        ).bounds(width / 2 - 100, height - FOOTER_HEIGHT + 6, 200, 20).build();
        this.addRenderableWidget(doneButton);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (VanillaSwitcherClient.isListeningForBind()) {
            int key = event.key() == SDLScancode.SDL_SCANCODE_ESCAPE ? -1 : event.key();
            VanillaSwitcherClient.captureBindKey(key);

            list.refreshBindLabels();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        VanillaSwitcherClient.setListeningFor(null);
        VanillaSwitcherClient.getConfig().save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (rebuildPending) {
            rebuildPending = false;
            list.rebuild();
            doneButton.setMessage(Component.literal(Text.DONE.in(language())));
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.literal(Text.TITLE.in(language())), this.width / 2, 12, 0xFFFFFFFF);
    }

    static final class ConfigList extends ContainerObjectSelectionList<ConfigList.Entry> {
        static final int ROW_WIDTH = 310;
        private static final int ROW_HEIGHT = 25;
        private static final int BUTTON_HEIGHT = 20;

        private static final String LANGUAGE_LABEL = "Language: ";

        private static final int SCALE_STEPS_PER_UNIT = 20;

        private static final int PAIR_GAP = 8;

        private final ModConfig cfg = VanillaSwitcherClient.getConfig();
        private final Runnable onLanguageChanged;
        private Button activateBindButton;
        private Button menuBindButton;

        ConfigList(Minecraft minecraft, int listWidth, int listHeight, int listY, Runnable onLanguageChanged) {
            super(minecraft, listWidth, listHeight, listY, ROW_HEIGHT);
            this.onLanguageChanged = onLanguageChanged;
            this.centerListVertically = false;
            populate();
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        void rebuild() {
            clearEntries();
            populate();
        }

        private void populate() {
            Language lang = cfg.language;

            addSectionHeader(Text.SECTION_VISUALS);
            addPairedToggles(
                    Text.PARTICLES, () -> cfg.disableParticles, v -> cfg.disableParticles = v,
                    Text.NO_FOG, () -> cfg.noFog, v -> cfg.noFog = v
            );
            addPairedToggles(
                    Text.FIRE_SHIELD_OFFSETS, () -> cfg.applyOffsets, v -> cfg.applyOffsets = v,
                    Text.ARMOR_HUD, () -> cfg.showArmorHud, v -> cfg.showArmorHud = v
            );
            addToggle(Text.SMOOTH_LIGHTING_SWITCH, () -> cfg.smoothLightingSwitch, v -> cfg.smoothLightingSwitch = v);

            addSectionHeader(Text.SECTION_BRIGHTNESS);
            addToggle(Text.SMOOTH_FULLBRIGHT, () -> cfg.smoothBrightness, v -> cfg.smoothBrightness = v);
            addSlider(
                    Text.FULLBRIGHT_STRENGTH,
                    1, 15, cfg.crystalBrightness,
                    value -> cfg.crystalBrightness = value,
                    value -> value + Text.STRENGTH_SUFFIX.in(lang)
            );
            addSlider(
                    Text.BRIGHTNESS_TRANSITION,
                    ModConfig.BRIGHTNESS_TRANSITION_MIN_SECONDS, ModConfig.BRIGHTNESS_TRANSITION_MAX_SECONDS,
                    Math.round(cfg.brightnessTransitionSeconds),
                    value -> cfg.brightnessTransitionSeconds = value,
                    value -> value + Text.SECONDS_SUFFIX.in(lang)
            );

            addSectionHeader(Text.SECTION_RENDER_DISTANCE);
            addToggle(Text.RENDER_DISTANCE_SWITCH, () -> cfg.switchRenderDistance, v -> cfg.switchRenderDistance = v);
            addSlider(
                    Text.RENDER_DISTANCE_ON,
                    2, 32, cfg.crystalRenderDistance,
                    value -> cfg.crystalRenderDistance = value,
                    value -> Text.chunks(lang, value)
            );
            addSlider(
                    Text.RENDER_DISTANCE_OFF,
                    2, 32, cfg.vanillaRenderDistance,
                    value -> cfg.vanillaRenderDistance = value,
                    value -> Text.chunks(lang, value)
            );

            addSectionHeader(Text.SECTION_TOTEM);
            addToggle(Text.TOTEM_RESIZE, () -> cfg.resizeTotem, v -> cfg.resizeTotem = v);
            addSlider(
                    Text.TOTEM_SIZE,
                    scaleToSteps(ModConfig.TOTEM_SCALE_MIN), scaleToSteps(ModConfig.TOTEM_SCALE_MAX),
                    scaleToSteps(cfg.totemHandScale),
                    steps -> cfg.totemHandScale = stepsToScale(steps),
                    ConfigList::formatScale
            );
            addSlider(
                    Text.TOTEM_POP_SIZE,
                    scaleToSteps(ModConfig.TOTEM_SCALE_MIN), scaleToSteps(ModConfig.TOTEM_SCALE_MAX),
                    scaleToSteps(cfg.totemPopScale),
                    steps -> cfg.totemPopScale = stepsToScale(steps),
                    ConfigList::formatScale
            );

            addSectionHeader(Text.SECTION_OTHERS);
            addToggle(Text.ALERT_TOAST, () -> cfg.showAlertToast, v -> cfg.showAlertToast = v);
            addLanguageButton();

            addSectionHeader(Text.SECTION_BINDS);
            activateBindButton = createBindButton(BindTarget.ACTIVATE);
            menuBindButton = createBindButton(BindTarget.MENU);
            addEntry(new WidgetEntry(activateBindButton));
            addEntry(new WidgetEntry(menuBindButton));
        }

        private static int scaleToSteps(float scale) {
            return Math.round(scale * SCALE_STEPS_PER_UNIT);
        }

        private static float stepsToScale(int steps) {
            return steps / (float) SCALE_STEPS_PER_UNIT;
        }

        private static String formatScale(int steps) {
            return String.format(Locale.ROOT, "%.2f", steps / (double) SCALE_STEPS_PER_UNIT);
        }

        private void addToggle(Text label, BooleanSupplier get, Consumer<Boolean> set) {
            addEntry(new WidgetEntry(createToggleButton(label, get, set, ROW_WIDTH)));
        }

        private void addPairedToggles(Text leftLabel, BooleanSupplier leftGet, Consumer<Boolean> leftSet,
                                      Text rightLabel, BooleanSupplier rightGet, Consumer<Boolean> rightSet) {
            int halfWidth = (ROW_WIDTH - PAIR_GAP) / 2;
            Button left = createToggleButton(leftLabel, leftGet, leftSet, halfWidth);
            Button right = createToggleButton(rightLabel, rightGet, rightSet, halfWidth);
            addEntry(new PairedEntry(left, right));
        }

        private Button createToggleButton(Text label, BooleanSupplier get, Consumer<Boolean> set, int width) {
            Language lang = cfg.language;
            return Button.builder(
                    toggleLabel(lang, label, get.getAsBoolean()),
                    b -> {
                        boolean next = !get.getAsBoolean();
                        set.accept(next);
                        b.setMessage(toggleLabel(lang, label, next));
                    }
            ).bounds(0, 0, width, BUTTON_HEIGHT).build();
        }

        private static Component toggleLabel(Language lang, Text label, boolean on) {
            return Component.literal(label.in(lang) + ": " + (on ? Text.ON : Text.OFF).in(lang));
        }

        private void addSlider(Text label, int min, int max, int current,
                               IntConsumer setter, IntFunction<String> valueText) {
            String labelText = label.in(cfg.language);
            OptionInstance<Integer> option = new OptionInstance<>(
                    labelText,
                    OptionInstance.cachedConstantTooltip(Component.literal(labelText)),
                    (caption, value) -> Component.literal(labelText + ": " + valueText.apply(value)),
                    new OptionInstance.IntRange(min, max),
                    Math.max(min, Math.min(max, current)),
                    setter::accept
            );
            addOption(option);
        }

        private void addOption(OptionInstance<?> option) {
            addEntry(new WidgetEntry(option.createButton(minecraft.options, 0, 0, ROW_WIDTH)));
        }

        private void addSectionHeader(Text label) {
            addEntry(new SectionHeaderEntry(minecraft.font, label.in(cfg.language)));
        }

        private void addLanguageButton() {
            Button button = Button.builder(
                    Component.literal(LANGUAGE_LABEL + cfg.language.displayName()),
                    b -> {
                        cfg.language = cfg.language.next();
                        onLanguageChanged.run();
                    }
            ).bounds(0, 0, ROW_WIDTH, BUTTON_HEIGHT).build();
            addEntry(new WidgetEntry(button));
        }

        private Button createBindButton(BindTarget target) {
            return Button.builder(
                    bindLabel(target),
                    button -> {
                        VanillaSwitcherClient.setListeningFor(target);
                        refreshBindLabels();
                    }
            ).bounds(0, 0, ROW_WIDTH, BUTTON_HEIGHT).build();
        }

        private Component bindLabel(BindTarget target) {
            Language lang = cfg.language;
            String name = (target == BindTarget.ACTIVATE ? Text.ACTIVATE_BIND : Text.MENU_BIND).in(lang);
            String state = VanillaSwitcherClient.getListeningFor() == target
                    ? Text.PRESS_A_KEY.in(lang)
                    : VanillaSwitcherClient.bindDisplayName(target);
            return Component.literal(name + ": " + state);
        }

        void refreshBindLabels() {
            if (activateBindButton != null) {
                activateBindButton.setMessage(bindLabel(BindTarget.ACTIVATE));
            }
            if (menuBindButton != null) {
                menuBindButton.setMessage(bindLabel(BindTarget.MENU));
            }
        }

        abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        }

        static final class WidgetEntry extends Entry {
            private final AbstractWidget widget;

            WidgetEntry(AbstractWidget widget) {
                this.widget = widget;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int x = getContentXMiddle() - widget.getWidth() / 2;
                widget.setPosition(x, getContentY());
                widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(widget);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(widget);
            }
        }

        static final class PairedEntry extends Entry {
            private final AbstractWidget left;
            private final AbstractWidget right;

            PairedEntry(AbstractWidget left, AbstractWidget right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int rowLeft = getContentXMiddle() - ROW_WIDTH / 2;
                int y = getContentY();
                left.setPosition(rowLeft, y);
                left.extractRenderState(graphics, mouseX, mouseY, partialTick);
                right.setPosition(rowLeft + ROW_WIDTH - right.getWidth(), y);
                right.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(left, right);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(left, right);
            }
        }

        static final class SectionHeaderEntry extends Entry {
            private static final int TEXT_HEIGHT = 9;

            private final Font font;
            private final Component label;

            SectionHeaderEntry(Font font, String text) {
                this.font = font;
                this.label = Component.literal(text);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int left = getContentXMiddle() - ROW_WIDTH / 2;
                int y = getContentY() + BUTTON_HEIGHT - TEXT_HEIGHT;
                graphics.text(font, label, left, y, 0xFFFFFFFF, false);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }
        }
    }
}
