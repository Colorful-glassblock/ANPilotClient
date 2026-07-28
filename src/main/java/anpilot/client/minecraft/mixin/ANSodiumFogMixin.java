package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium 雾渲染兼容 Mixin
 * 注入 Sodium 注入到 FogRenderer 的 sodium$getFogParameters 方法
 * 当 NoRender.fog 开启时返回 FogParameters.NONE, 彻底移除雾效
 */
@Pseudo
@Mixin(value = FogRenderer.class, priority = 1100)
public class ANSodiumFogMixin {

    @Inject(method = "sodium$getFogParameters", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetFogParameters(CallbackInfoReturnable<FogParameters> cir) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            var moduleManager = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager();
            if (moduleManager != null) {
                var noRender = moduleManager.noRender();
                if (noRender != null && noRender.getEnabled() && noRender.getFog().getValue()) {
                    cir.setReturnValue(FogParameters.NONE);
                }
            }
        }
    }
}
