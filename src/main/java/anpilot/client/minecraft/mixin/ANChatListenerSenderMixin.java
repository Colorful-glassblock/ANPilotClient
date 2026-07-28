package anpilot.client.minecraft.mixin;

import anpilot.client.features.module.misc.ANChatUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;

@Mixin(ChatListener.class)
public abstract class ANChatListenerSenderMixin {
    @Inject(method = "showMessageToPlayer", at = @At("HEAD"))
    private void anpilot$beginSender(
        ChatType.Bound bound,
        PlayerChatMessage message,
        Component decorated,
        GameProfile sender,
        boolean onlyShowSecureChat,
        Instant receptionTimestamp,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ANChatUtils.beginChatSender(sender);
    }

    @Inject(method = "showMessageToPlayer", at = @At("RETURN"))
    private void anpilot$endSender(
        ChatType.Bound bound,
        PlayerChatMessage message,
        Component decorated,
        GameProfile sender,
        boolean onlyShowSecureChat,
        Instant receptionTimestamp,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ANChatUtils.endChatSender();
    }

    @ModifyArg(
        method = "showMessageToPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"
        ),
        index = 0
    )
    private Component anpilot$decoratePlayerDisplay(Component component) {
        return ANChatUtils.decoratePlayerDisplay(component);
    }
}
