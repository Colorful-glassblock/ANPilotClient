package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
public abstract class ANScreenEffectRendererMixin {
    @WrapOperation(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderTex(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void noWallOverlay(TextureAtlasSprite sprite, PoseStack poseStack, MultiBufferSource bufferSource, Operation<Void> original) {
        if (ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getNoWallOverlay().getValue()) {
            return;
        }

        original.call(sprite, poseStack, bufferSource);
    }

    @WrapOperation(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
    private void noFireOverlay(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, Operation<Void> original) {
        if (ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getNoFireOverlay().getValue()) {
            return;
        }

        original.call(poseStack, bufferSource, sprite);
    }
}
