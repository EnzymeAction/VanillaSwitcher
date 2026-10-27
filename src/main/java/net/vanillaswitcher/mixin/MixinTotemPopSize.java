package net.vanillaswitcher.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.vanillaswitcher.VanillaSwitcherClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ScreenEffectRenderer.class)
public class MixinTotemPopSize {
    @ModifyArgs(
            method = "renderItemActivationAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"
            )
    )
    private void vs$scaleTotemPop(Args args) {
        if (!VanillaSwitcherClient.shouldResizeTotem()) {
            return;
        }

        float scale = VanillaSwitcherClient.totemPopScale();
        for (int i = 0; i < 3; i++) {
            float original = args.get(i);
            args.set(i, original * scale);
        }
    }
}
