package net.vanillaswitcher.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.vanillaswitcher.VanillaSwitcherClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the first-person fire overlay down (the "low fire" effect).
 * <p>
 * On 1.21.11 the overlay is drawn by the static {@code renderFire(PoseStack, MultiBufferSource,
 * TextureAtlasSprite)} (26.x renamed it to {@code submitFire} and switched to a
 * {@code SubmitNodeCollector}). The translation is applied right after the method's own
 * {@code pushPose()}, like BactroMod's 1.21.11 low-fire mixin, so the matching {@code popPose()}
 * undoes it again.
 */
@Mixin(ScreenEffectRenderer.class)
public class MixinScreenEffectRenderer {

    @Inject(
            method = "renderFire",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private static void vs$offsetFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        poseStack.translate(0, VanillaSwitcherClient.fireOffset() / 100F, 0);
    }
}
