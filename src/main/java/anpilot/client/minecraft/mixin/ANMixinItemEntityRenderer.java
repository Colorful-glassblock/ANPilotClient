package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANDropsESP;
import anpilot.client.minecraft.mixin.accessor.ANItemStackRenderStateAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ANMixinItemEntityRenderer {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
    private void onConstantScaleRender(ItemEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci) {
        ANDropsESP.renderingDroppedItem = true;
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        var dropsESP = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().dropsESP();
        if (dropsESP == null || !dropsESP.getEnabled()) return;

        double dx = state.x - camera.pos.x;
        double dy = state.y - camera.pos.y;
        double dz = state.z - camera.pos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float scale = dropsESP.scaleFor(distance);
        matrices.scale(scale, scale, scale);
        if (!dropsESP.bobbingEnabled()) {
            state.bobOffset = -state.ageInTicks / 10.0f;
        }
        int tintColor = dropsESP.tintColor();
        ANItemStackRenderStateAccessor item = (ANItemStackRenderStateAccessor) state.item;
        var layers = item.anpilot$getLayers();
        for (int i = 0; i < item.anpilot$getActiveLayerCount(); i++) {
            layers[i].tintLayers().add(tintColor);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void onConstantScaleRenderTail(ItemEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci) {
        ANDropsESP.renderingDroppedItem = false;
    }
}
