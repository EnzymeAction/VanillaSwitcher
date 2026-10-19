package net.vanillaswitcher.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * The settings screen. Everything sits in one scrolling list, split into sections with a
 * plain left-aligned text header above each one - no background box, matching vanilla's own
 * Video Settings screen (e.g. "Display", "Quality &amp; Performance") - rather than a divider
 * line. Only the Done button is pinned to the bottom:
 * <ol>
 *   <li>Visuals: particles, fog, fire/shield offsets, armor HUD, smooth lighting switch -
 *       paired two-to-a-row where the labels are short enough, same as vanilla's own Video
 *       Settings screen, with the longest label left full width</li>
 *   <li>Brightness</li>
 *   <li>Render Distance</li>
 *   <li>Totem</li>
 *   <li>Others: alert toast, language</li>
 *   <li>Binds: activate, open menu</li>
 * </ol>
 */
public class ConfigScreen extends Screen {

    /** Space reserved above the list for the title, and below it for the Done button. */
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 32;

    private final Screen parent;
    private ConfigList list;
    private Button doneButton;
    /** Set by the language button; handled at the start of the next frame (never in the middle of a click). */
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
        // The list fills everything between the title and the Done button.
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
            // Esc clears the bind (like vanilla's Controls screen) instead of binding Esc itself.
            int key = event.key() == GLFW.GLFW_KEY_ESCAPE ? -1 : event.key();
            VanillaSwitcherClient.captureBindKey(key);
            // Also refreshes the *other* button: binding a key that the other bind used unbinds it.
            list.refreshBindLabels();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        VanillaSwitcherClient.setListeningFor(null);
        VanillaSwitcherClient.getConfig().save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (rebuildPending) {
            rebuildPending = false;
            list.rebuild();
            doneButton.setMessage(Component.literal(Text.DONE.in(language())));
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, Component.literal(Text.TITLE.in(language())), this.width / 2, 12, 0xFFFFFFFF);
    }

    static final class ConfigList extends ContainerObjectSelectionList<ConfigList.Entry> {

        static final int ROW_WIDTH = 310;
        private static final int ROW_HEIGHT = 25;
        private static final int BUTTON_HEIGHT = 20;
        /** The language button always reads "Language: ..." regardless of the selected language. */
        private static final String LANGUAGE_LABEL = "Language: ";
        /** The totem size sliders move in steps of 0.05 (so 0.40, 0.45, 0.50, ...). */
        private static final int SCALE_STEPS_PER_UNIT = 20;
        /** Horizontal gap between two paired (half-width) toggles in the same row. */
        private static final int PAIR_GAP = 8;

        private final ModConfig cfg = VanillaSwitcherClient.getConfig();
        private final Runnable onLanguageChanged;
        private Button activateBindButton;
        private Button menuBindButton;

        /**
         * The super constructor is (client, width, HEIGHT, Y, itemHeight) - this is the argument
         * order used by vanilla's own list classes.
         */
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

        /** Rebuilds every row (used after a language change). Same list widget, so the scroll position stays. */
        void rebuild() {
            clearEntries();
            populate();
        }

        private void populate() {
            Language lang = cfg.language;

            // --- Visuals ---
            // Paired two-to-a-row (thinner, like vanilla's Quality & Performance grid) wherever
            // the labels are short enough; Smooth Lighting Switch's label is too long for half
            // width, so it stays a full-width row like "Weather Effect Radius" does in vanilla.
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

            // --- Brightness ---
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

            // --- Render Distance ---
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

            // --- Totem ---
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

            // --- Others: whatever doesn't fit a category above ---
            addSectionHeader(Text.SECTION_OTHERS);
            addToggle(Text.ALERT_TOAST, () -> cfg.showAlertToast, v -> cfg.showAlertToast = v);
            addLanguageButton();

            // --- Binds: last, so it doesn't get in the way of the settings above ---
            addSectionHeader(Text.SECTION_BINDS);
            activateBindButton = createBindButton(BindTarget.ACTIVATE);
            menuBindButton = createBindButton(BindTarget.MENU);
            addEntry(new WidgetEntry(activateBindButton));
            addEntry(new WidgetEntry(menuBindButton));
        }

        // --- helpers for the two totem sliders ---

        private static int scaleToSteps(float scale) {
            return Math.round(scale * SCALE_STEPS_PER_UNIT);
        }

        private static float stepsToScale(int steps) {
            return steps / (float) SCALE_STEPS_PER_UNIT;
        }

        private static String formatScale(int steps) {
            return String.format(Locale.ROOT, "%.2f", steps / (double) SCALE_STEPS_PER_UNIT);
        }

        // --- row builders ---

        private void addToggle(Text label, BooleanSupplier get, Consumer<Boolean> set) {
            addEntry(new WidgetEntry(createToggleButton(label, get, set, ROW_WIDTH)));
        }

        /**
         * Two toggles side by side in one row, each half the usual width (minus a small gap
         * between them) - the "thinner" layout vanilla uses in its Quality &amp; Performance
         * grid, instead of one full-width row per toggle.
         */
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

        /**
         * @param valueText turns the slider's integer value into the text shown after the label,
         *                  e.g. 3 -> "3/15" or 8 -> "0.40"
         */
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

        // --- key bind buttons ---

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

        // --- rows ---

        abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        }

        /** One full-width row holding a single widget (a toggle, a slider, or a button). */
        static final class WidgetEntry extends Entry {
            private final AbstractWidget widget;

            WidgetEntry(AbstractWidget widget) {
                this.widget = widget;
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int x = getContentXMiddle() - widget.getWidth() / 2;
                widget.setPosition(x, getContentY());
                widget.render(graphics, mouseX, mouseY, partialTick);
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

        /**
         * One row holding two widgets side by side - the left one flush with the row's left
         * edge, the right one flush with its right edge, a small gap between them. Used for the
         * "thinner" paired toggles in the Visuals section.
         */
        static final class PairedEntry extends Entry {
            private final AbstractWidget left;
            private final AbstractWidget right;

            PairedEntry(AbstractWidget left, AbstractWidget right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int rowLeft = getContentXMiddle() - ROW_WIDTH / 2;
                int y = getContentY();
                left.setPosition(rowLeft, y);
                left.render(graphics, mouseX, mouseY, partialTick);
                right.setPosition(rowLeft + ROW_WIDTH - right.getWidth(), y);
                right.render(graphics, mouseX, mouseY, partialTick);
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

        /**
         * A section header row: plain left-aligned text, no background box - matching how
         * vanilla's own Video Settings screen labels its groups ("Display",
         * "Quality &amp; Performance", "Preferences") rather than a divider line or a filled
         * box. Nothing to click or narrate.
         * <p>
         * The text sits at the bottom of its row rather than centered. Every row already has a
         * fixed-size gap below it before the next row starts, so centering the text split that
         * gap evenly above and below it; vanilla instead keeps a header close to the row right
         * after it (its own section) and leaves the larger gap above it (the end of the
         * previous section). Pinning the text to the bottom reproduces that: the small gap
         * after comes only from the row's own trailing gap, while the big gap before is that
         * same trailing gap plus all the empty space this row isn't using above the text.
         */
        static final class SectionHeaderEntry extends Entry {
            private static final int TEXT_HEIGHT = 9;

            private final Font font;
            private final Component label;

            SectionHeaderEntry(Font font, String text) {
                this.font = font;
                this.label = Component.literal(text);
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                       boolean hovered, float partialTick) {
                int left = getContentXMiddle() - ROW_WIDTH / 2;
                int y = getContentY() + BUTTON_HEIGHT - TEXT_HEIGHT;
                graphics.drawString(font, label, left, y, 0xFFFFFFFF, false);
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
