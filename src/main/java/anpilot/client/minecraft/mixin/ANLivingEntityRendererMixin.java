package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.renderer.utils.IANEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class ANLivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Shadow
    public abstract Identifier getTextureLocation(S state);

    @Redirect(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private <MS> void anpilot$submitChamsLayer(
        SubmitNodeCollector collector,
        Model<? super MS> model,
        MS state,
        PoseStack poseStack,
        RenderType renderType,
        int light,
        int overlay,
        int color,
        TextureAtlasSprite sprite,
        int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        RenderType finalRenderType = renderType;
        int finalColor = color;

        if (ANServiceRegistry.INSTANCE.isInitialized() && state instanceof LivingEntityRenderState livingState) {
            LivingEntity entity = anpilot$entityFromState(livingState);
            if (entity != null && entity.getClass().getSimpleName().equals("LogoutPlayerEntity")) {
                Identifier texture = getTextureLocation((S) livingState);
                finalRenderType = net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
                finalColor = (color & 0x00FFFFFF) | 0x66000000;
            }
        }

        collector.submitModel(model, state, poseStack, finalRenderType, light, overlay, finalColor, sprite, outlineColor, crumblingOverlay);

        if (!ANServiceRegistry.INSTANCE.isInitialized() || !(state instanceof LivingEntityRenderState livingState)) return;
        LivingEntity entity = anpilot$entityFromState(livingState);
        if (entity == null) return;

        Identifier texture = getTextureLocation((S) livingState);
        var chams = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().chams();
        if (chams != null && chams.getEnabled() && chams.shouldOverlayTexture(entity)) {
            collector.submitModel(
                model,
                state,
                poseStack,
                chams.renderType(texture),
                0x00F000F0,
                overlay,
                chams.colorFor(entity),
                sprite,
                chams.outlineColorFor(entity),
                crumblingOverlay
            );
        }
    }

    @Unique
    private LivingEntity anpilot$entityFromState(LivingEntityRenderState state) {
        if (state instanceof IANEntityRenderState renderState && renderState.an$getEntity() instanceof LivingEntity entity) {
            return entity;
        }
        return null;
    }
}
