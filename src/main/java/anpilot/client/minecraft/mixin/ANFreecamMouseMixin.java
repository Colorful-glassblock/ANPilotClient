package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANFreecam;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam 鼠标 Mixin
 * 拦截鼠标移动, 用于 Freecam 模式的视角控制
 */
@Mixin(MouseHandler.class)
public abstract class ANFreecamMouseMixin {

    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"), cancellable = true)
    private void onMouseUpdate(CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        if (net.minecraft.client.Minecraft.getInstance().screen != null) return;
        ANFreecam freecam = (ANFreecam) ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Freecam");
        if (freecam == null || !freecam.getEnabled()) return;

        double dx = this.accumulatedDX;
        double dy = this.accumulatedDY;
        this.accumulatedDX = 0;
        this.accumulatedDY = 0;

        freecam.onMouseMove(dx, dy);
        ci.cancel();
    }
}
