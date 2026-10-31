package net.vanillaswitcher.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.vanillaswitcher.VanillaSwitcherClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void vs$blockParticles(ParticleOptions options, double x, double y, double z,
                                   double velocityX, double velocityY, double velocityZ,
                                   CallbackInfoReturnable<Particle> cir) {
        if (VanillaSwitcherClient.shouldDisableParticles() && !vs$isUnsafeToBlock(options)) {
            cir.setReturnValue(null);
        }
    }

    private static boolean vs$isUnsafeToBlock(ParticleOptions options) {
        return options.getType() == ParticleTypes.FIREWORK;
    }
}
