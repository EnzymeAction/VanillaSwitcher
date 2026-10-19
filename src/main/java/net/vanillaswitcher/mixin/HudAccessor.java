package net.vanillaswitcher.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets us reuse vanilla's own hotbar-slot background sprite for the armor HUD,
 * so it looks like a native part of the hotbar instead of a floating icon row.
 * <p>
 * On 1.21.11 the in-game HUD class is {@code Gui} (it was renamed to {@code Hud} on 26.x), and the
 * field is {@code private static final Identifier HOTBAR_SPRITE}. Confirmed against
 * uku3lig/armor-hud's own "1.21.11" branch, which shadows exactly that field.
 */
@Mixin(Gui.class)
public interface HudAccessor {

    @Accessor("HOTBAR_SPRITE")
    static Identifier vs$hotbarSprite() {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
