package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.event.impl.RenderAfterWorldEvent;
import anpilot.client.renderer.render.ANRender3DCenter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ANGameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private void bobHurt(CameraRenderState cameraRenderState, PoseStack poseStack) { }

    @Shadow
    private void bobView(CameraRenderState cameraRenderState, PoseStack poseStack) { }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Lnet/minecraft/client/renderer/fog/FogData;)V", shift = At.Shift.BEFORE))
    private void updateANRenderCenter(DeltaTracker tickCounter, CallbackInfo info) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;

        CameraRenderState camera = minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f projection = new Matrix4f(camera.projectionMatrix);
        PoseStack matrices = new PoseStack();
        bobHurt(camera, matrices);
        if (minecraft.gameRenderer.getGameRenderState().optionsRenderState.bobView) {
            bobView(camera, matrices);
        }
        projection.mul(matrices.last().pose());
        ANRender3DCenter.INSTANCE.update(projection, camera.viewRotationMatrix, camera.pos);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderWorldTail(DeltaTracker tickCounter, CallbackInfo info) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new RenderAfterWorldEvent(null));
        }
    }

    @ModifyVariable(method = "renderLevel", at = @At("STORE"), ordinal = 3)
    private float onNauseaIntensity(float original) {
        if (ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getNoNausea().getValue()) {
            return 0.0f;
        }
        return original;
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bobViewHook(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo ci) {
        if (ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noBobView() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noBobView().getEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void bobHurtHook(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo ci) {
        if (ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getNoHurtCamera().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.is(Items.TOTEM_OF_UNDYING) &&
            ANServiceRegistry.INSTANCE.isInitialized() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender() != null &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getEnabled() &&
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noRender().getNoTotemAnimation().getValue()) {
            info.cancel();
        }
    }


}
