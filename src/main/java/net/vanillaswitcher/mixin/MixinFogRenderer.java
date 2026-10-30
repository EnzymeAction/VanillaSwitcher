package net.vanillaswitcher.mixin;

import net.vanillaswitcher.VanillaSwitcherClient;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;

@Mixin(value = FogRenderer.class, priority = 1500)
public abstract class MixinFogRenderer {
    @Shadow
    @Final
    private static List<FogEnvironment> FOG_ENVIRONMENTS;

    @Inject(
            method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceEnd:F", ordinal = 0, shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void vs$suppressFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level,
                                CallbackInfoReturnable<Vector4f> cir,
                                float partialTick, Vector4f fogColor, float renderDistanceInBlocks, FogType fogType, Entity entity, FogData fog) {
        if (!VanillaSwitcherClient.shouldDisableFog()) {
            return;
        }

        for (int i = 0; i < FOG_ENVIRONMENTS.size(); ++i) {
            if (FOG_ENVIRONMENTS.get(i).isApplicable(fogType, entity)) {
                fog.environmentalStart = Float.MAX_VALUE;
                fog.environmentalEnd = Float.MAX_VALUE;
                fog.renderDistanceStart = Float.MAX_VALUE;
                fog.renderDistanceEnd = Float.MAX_VALUE;

                fog.skyEnd = i == 5 ? Mth.clamp(renderDistanceInBlocks, 2 * 16, 32 * 16) : Float.MAX_VALUE;
                fog.cloudEnd = i == 5 ? Minecraft.getInstance().options.cloudRange().get() * 16 : Float.MAX_VALUE;

                break;
            }
        }
    }
}
