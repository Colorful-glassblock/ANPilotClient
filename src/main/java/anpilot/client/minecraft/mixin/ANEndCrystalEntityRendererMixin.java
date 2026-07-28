package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANModels;
import anpilot.client.renderer.utils.IANEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EndCrystalRenderer.class)
public abstract class ANEndCrystalEntityRendererMixin {
    @ModifyArgs(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V")
    )
    private void onSubmitScale(Args args) {
        ANModels models = anpilot$models();
        if (models == null || !models.getEnabled()) return;

        float scale = models.getCrystalScale().getValue();
        args.set(0, ((Float) args.get(0)) * scale);
        args.set(1, ((Float) args.get(1)) * scale);
        args.set(2, ((Float) args.get(2)) * scale);
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;F)V",
            at = @At("TAIL")
    )
    private void onExtractRenderState(EndCrystal entity, EndCrystalRenderState state, float partialTicks, CallbackInfo ci) {
        ANModels models = anpilot$models();
        if (models == null || !models.getEnabled()) return;

        state.ageInTicks *= models.getCrystalSpin().getValue();
    }

    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private <S> void onSubmitModel(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, Identifier texture, int light, int overlay, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        collector.submitModel(model, state, poseStack, texture, light, overlay, outlineColor, crumblingOverlay);

        if (!ANServiceRegistry.INSTANCE.isInitialized() || !(state instanceof IANEntityRenderState renderState)) {
            return;
        }

        Entity entity = renderState.an$getEntity();
        var chams = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().chams();
        if (chams == null || !chams.getEnabled() || !chams.shouldOverlayTexture(entity)) {
            return;
        }

        collector.submitModel(
            model,
            state,
            poseStack,
            chams.renderType(texture),
            0x00F000F0,
            overlay,
            chams.colorFor(entity),
            null,
            chams.outlineColorFor(entity),
            crumblingOverlay
        );
    }

    @Unique
    private static ANModels anpilot$models() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return null;
        return ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().models();
    }
}
