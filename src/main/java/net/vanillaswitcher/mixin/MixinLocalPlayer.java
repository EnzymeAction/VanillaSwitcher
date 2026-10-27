package net.vanillaswitcher.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.vanillaswitcher.VanillaSwitcherClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @WrapOperation(
            method = "displayItemActivation",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F")
    )
    private float vs$centerTotemPop(RandomSource instance, Operation<Float> original) {
        float value = original.call(instance);
        return VanillaSwitcherClient.shouldResizeTotem() ? 0.5F : value;
    }
}
