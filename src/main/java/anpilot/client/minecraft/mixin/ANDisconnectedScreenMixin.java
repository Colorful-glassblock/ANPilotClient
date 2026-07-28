package anpilot.client.minecraft.mixin;

import anpilot.client.features.gui.ANLeaveGuiState;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class ANDisconnectedScreenMixin {
    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("TAIL"))
    private void onInit(Screen parent, Component title, DisconnectionDetails details, CallbackInfo ci) {
        if (details.reason() != null) {
            ANLeaveGuiState.INSTANCE.setReason(details.reason().getString());
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/DisconnectionDetails;Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void onInitWithButton(Screen parent, Component title, DisconnectionDetails details, Component buttonText, CallbackInfo ci) {
        if (details.reason() != null) {
            ANLeaveGuiState.INSTANCE.setReason(details.reason().getString());
        }
    }
}
