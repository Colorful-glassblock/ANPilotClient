package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANFreecam;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freecam 相机 Mixin
 * 覆盖相机位置和旋转为 Freecam 模块的值
 */
@Mixin(Camera.class)
public abstract class ANFreecamCameraMixin {

    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "alignWithEntity", at = @At("HEAD"), cancellable = true)
    private void onAlignWithEntity(float partialTicks, CallbackInfo ci) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        ANFreecam freecam = (ANFreecam) ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("Freecam");
        if (freecam == null || !freecam.getEnabled()) return;

        Vec3 pos = freecam.getRenderPosition(partialTicks);
        if (pos == null) return;

        this.detached = true;
        setPosition(pos.x, pos.y, pos.z);
        setRotation(freecam.getYaw(), freecam.getPitch());
        ci.cancel();
    }
}
