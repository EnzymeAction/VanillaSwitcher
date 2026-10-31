package net.vanillaswitcher.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.vanillaswitcher.VanillaSwitcherClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinTotemPopPosition {
    @Shadow
    private float itemActivationOffX;

    @Shadow
    private float itemActivationOffY;

    @Inject(method = "displayItemActivation", at = @At("TAIL"))
    private void vs$centerTotemPop(ItemStack stack, RandomSource random, CallbackInfo ci) {
        if (VanillaSwitcherClient.shouldResizeTotem()) {
            this.itemActivationOffX = 0.0F;
            this.itemActivationOffY = 0.0F;
        }
    }
}
