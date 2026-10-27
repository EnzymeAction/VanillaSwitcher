package net.vanillaswitcher.hud;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.vanillaswitcher.mixin.HudAccessor;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHud {
    private static final int SIZE = 22;
    private static final int STEP = 20;
    private static final int HOTBAR_OFFSET = 98;

    private static final int OFFHAND_OFFSET = 29;

    private static final int ATTACK_INDICATOR_OFFSET = 23;

    private ArmorHud() {
    }

    private static int extraHotbarOffset(Minecraft mc, Player player) {
        if (player.getMainArm() == HumanoidArm.LEFT) {
            boolean indicatorShown = mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR
                    && player.getAttackStrengthScale(0) < 1;
            return indicatorShown ? ATTACK_INDICATOR_OFFSET : 0;
        }
        return player.getOffhandItem().isEmpty() ? 0 : OFFHAND_OFFSET;
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, Minecraft mc,
                              int screenWidth, int screenHeight) {
        Player player = mc.player;
        if (player == null) {
            return;
        }

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<ItemStack> armorItems = new ArrayList<>(4);
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                armorItems.add(stack);
            }
        }

        if (armorItems.isEmpty()) {
            return;
        }

        int textureWidth = SIZE + ((armorItems.size() - 1) * STEP);
        int baseX = screenWidth / 2 - (HOTBAR_OFFSET + extraHotbarOffset(mc, player)) - textureWidth;
        int baseY = screenHeight - SIZE;
        int color = 0xFFFFFFFF;

        Identifier hotbarSprite;
        try {
            hotbarSprite = HudAccessor.vs$hotbarSprite();
        } catch (Throwable t) {
            hotbarSprite = null;
        }

        if (hotbarSprite != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hotbarSprite, 182, 22, 0, 0, baseX, baseY, textureWidth - 3, SIZE, color);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hotbarSprite, 182, 22, 182 - 3, 0, baseX + textureWidth - 3, baseY, 3, SIZE, color);
        }

        for (int i = 0; i < armorItems.size(); i++) {
            ItemStack stack = armorItems.get(i);
            int x = baseX + STEP * i + 3;
            int y = baseY + 3;
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }
    }
}
