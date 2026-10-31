package net.vanillaswitcher.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface HudAccessor {

    @Accessor("HOTBAR_SPRITE")
    static Identifier vs$hotbarSprite() {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
