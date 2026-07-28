package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class ANMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void onButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo callback) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;

        if (action == GLFW.GLFW_PRESS) {
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().onMousePressed(buttonInfo.button());
        } else if (action == GLFW.GLFW_RELEASE) {
            ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().onMouseReleased(buttonInfo.button());
        }
    }
}
