package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANModels;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndCrystalModel.class)
public abstract class ANEndCrystalModelMixin {
    @ModifyExpressionValue(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EndCrystalRenderer;getY(F)F")
    )
    private float onCrystalYOffset(float original) {
        ANModels models = anpilot$models();
        if (models == null || !models.getEnabled() || models.getCrystalBounce().getValue()) return original;
        return EndCrystalRenderer.getY(0.0f);
    }

    @Unique
    private static ANModels anpilot$models() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return null;
        return ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().models();
    }
}
