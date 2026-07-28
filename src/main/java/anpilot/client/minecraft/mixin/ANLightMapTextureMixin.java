package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class ANLightMapTextureMixin {
    @Inject(method = "calculateDarknessScale", at = @At("HEAD"), cancellable = true)
    private void getDarknessFactor(LivingEntity entity, float factor, float tickProgress, CallbackInfoReturnable<Float> info) {
        if (ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getDarkness().getValue()) {
            info.setReturnValue(0.0f);
        }
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void fullbright(LightmapRenderState state, float tickProgress, CallbackInfo ci) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            boolean fullbrightEnabled = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Fullbright") != null &&
                    ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Fullbright").getEnabled();
            boolean xrayEnabled = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("XRay") != null &&
                    ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("XRay").getEnabled();
            
            if (fullbrightEnabled || xrayEnabled) {
                state.needsUpdate = true;
                state.blockFactor = 15.0f;
                state.skyFactor = 15.0f;
                state.brightness = 1.0f;
                state.darknessEffectScale = 0.0f;
                // 模拟夜视效果: 强制全亮, 包括无光照区域
                state.nightVisionEffectIntensity = 1.0f;
                state.nightVisionColor = new Vector3f(1.0f, 1.0f, 1.0f);
            }
        }
    }
}
