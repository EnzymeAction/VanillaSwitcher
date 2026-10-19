package net.vanillaswitcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightTexture;
import net.vanillaswitcher.VanillaSwitcherClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * BactroMod-style fullbright for 1.21.11.
 * <p>
 * BactroMod multiplies the gamma value that {@code LightTexture#updateLightTexture} reads from
 * {@code Options.gamma()} (the second {@code Double.floatValue()} call in that method) rather than
 * changing the player's gamma option. We do the same, but animate the multiplier from 1 to the
 * configured crystal value.
 * <p>
 * Uses MixinExtras' {@code @ModifyExpressionValue} (bundled with Fabric Loader) instead of a plain
 * {@code @Redirect}, so it keeps working next to other mods that touch the same call.
 */
@Mixin(LightTexture.class)
public class MixinLightTexture {

    @ModifyExpressionValue(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Double;floatValue()F",
                    ordinal = 1
            )
    )
    private float vs$modifyGammaMultiplier(float original) {
        return original * VanillaSwitcherClient.brightnessMultiplier();
    }
}
