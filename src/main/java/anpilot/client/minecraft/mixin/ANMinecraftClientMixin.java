package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.event.impl.EventAttack;
import anpilot.client.features.event.impl.EventPostTick;
import anpilot.client.features.event.impl.EventPreTick;
import anpilot.client.features.event.impl.ResourcePacksReloadedEvent;
import anpilot.client.minecraft.gui.ANLeaveScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(value = Minecraft.class, priority = 1100)
public abstract class ANMinecraftClientMixin {
    @Shadow
    private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPreTick(CallbackInfo info) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new EventPreTick());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPostTick(CallbackInfo info) {
        if (ANServiceRegistry.INSTANCE.isInitialized()) {
            ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new EventPostTick());
            var fastUse = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().fastUse();
            if (fastUse != null && fastUse.getEnabled()) {
                Minecraft minecraft = (Minecraft)(Object)this;
                ItemStack stack = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
                rightClickDelay = fastUse.getItemUseCooldown(stack);
            }
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        EventAttack event = new EventAttack(null);
        ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(event);
        if (event.isCancelled()) cir.setReturnValue(false);
    }

    @Inject(method = "reloadResourcePacks", at = @At("RETURN"))
    private void onReloadResources(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        cir.getReturnValue().thenRun(() -> ANServiceRegistry.INSTANCE.getRuntime().getEventBus().post(new ResourcePacksReloadedEvent()));
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof DisconnectedScreen)) return;
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return;
        var leaveInfo = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("LeaveInfo");
        if (leaveInfo == null || !leaveInfo.getEnabled()) return;

        Minecraft minecraft = (Minecraft)(Object)this;
        minecraft.setScreen(new ANLeaveScreen());
        ci.cancel();
    }

    @Redirect(method = "updateTitle", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setTitle(Ljava/lang/String;)V"))
    private void setTitle(Window window, String original) {
        window.setTitle("ANPilotClient");
    }
}
