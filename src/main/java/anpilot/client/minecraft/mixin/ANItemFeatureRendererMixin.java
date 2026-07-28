package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANDropsESP;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.ANPilotRenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
public abstract class ANItemFeatureRendererMixin {
    @Unique
    private final QuadInstance anpilot$dropsEspQuadInstance = new QuadInstance();

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void anpilot$renderDropsEspOverlay(MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource, SubmitNodeStorage.ItemSubmit itemSubmit, CallbackInfo ci) {
        if (!ANDropsESP.renderingDroppedItem) return;
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;

        var dropsESP = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().dropsESP();
        if (dropsESP == null || !dropsESP.getEnabled()) return;

        int color = dropsESP.tintColor();
        if ((color >>> 24) == 0) return;

        PoseStack.Pose pose = itemSubmit.pose();
        var quads = itemSubmit.quads();
        anpilot$dropsEspQuadInstance.setLightCoords(0x00F000F0);
        anpilot$dropsEspQuadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
        anpilot$dropsEspQuadInstance.setColor(color);
        for (var quad : quads) {
            bufferSource.getBuffer(ANPilotRenderTypes.dropsEspItem(quad.materialInfo().sprite().atlasLocation()))
                .putBakedQuad(pose, quad, anpilot$dropsEspQuadInstance);
        }
    }
}
