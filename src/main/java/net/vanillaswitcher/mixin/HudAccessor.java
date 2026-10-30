package net.vanillaswitcher.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface HudAccessor {
    @Accessor("HOTBAR_SPRITE")
    static Identifier vs$hotbarSprite() {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
