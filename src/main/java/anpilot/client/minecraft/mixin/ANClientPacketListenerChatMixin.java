package anpilot.client.minecraft.mixin;

import anpilot.client.features.module.misc.ANChatUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ANClientPacketListenerChatMixin {
    @Inject(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void anpilot$blockLeakedCoordinates(String message, CallbackInfo ci) {
        if (ANChatUtils.shouldBlockOutgoingChat(message)) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String anpilot$encodeOutgoingChat(String message) {
        return ANChatUtils.transformOutgoingChat(message);
    }

    @ModifyArg(
        method = "handleSystemChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        ),
        index = 0
    )
    private Component anpilot$decorateSystemChat(Component component) {
        return ANChatUtils.decorateSystemChat(component);
    }

    @ModifyArg(
        method = "handleDisguisedChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleDisguisedChatMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType$Bound;)V"
        ),
        index = 0
    )
    private Component anpilot$decorateDisguisedChat(Component component) {
        return ANChatUtils.decorateDisguisedChat(component);
    }

    @ModifyArg(
        method = "handlePlayerChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handlePlayerChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/chat/ChatType$Bound;)V"
        ),
        index = 0
    )
    private PlayerChatMessage anpilot$decoratePlayerChat(PlayerChatMessage message) {
        return ANChatUtils.decoratePlayerChat(message);
    }
}
