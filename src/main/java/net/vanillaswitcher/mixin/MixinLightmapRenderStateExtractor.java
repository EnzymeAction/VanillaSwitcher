package net.vanillaswitcher.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.vanillaswitcher.VanillaSwitcherClient;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmapRenderStateExtractor {
    @Shadow
    private boolean needsUpdate;

    @Unique
    private float vs$brightnessMultiplier = -1.0F;

    @Inject(method = "extract", at = @At("HEAD"))
    private void vs$refreshChangedBrightness(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        float multiplier = VanillaSwitcherClient.brightnessMultiplier();
        if (multiplier == this.vs$brightnessMultiplier) {
            return;
        }

        this.vs$brightnessMultiplier = multiplier;
        this.needsUpdate = true;
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void vs$applyBrightnessBoost(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (!renderState.needsUpdate) {
            return;
        }

        float strength = (this.vs$brightnessMultiplier - 1.0F) / 14.0F;
        strength *= strength * strength;
        if (strength <= 0.0F) {
            return;
        }

        renderState.ambientColor = new Vector3f(renderState.ambientColor).lerp(LightmapRenderStateExtractor.WHITE, strength);
    }
}
