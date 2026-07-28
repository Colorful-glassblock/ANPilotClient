package anpilot.client.minecraft.mixin;

import anpilot.client.features.module.misc.ANChatUtils;
import anpilot.client.minecraft.duck.ANGuiMessageExt;
import anpilot.client.minecraft.duck.ANGuiMessageLineExt;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ANChatComponentMessageMixin {
    @Shadow
    @Final
    private java.util.List<net.minecraft.client.multiplayer.chat.GuiMessage.Line> trimmedMessages;

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD")
    )
    private void anpilot$onAddMessage(
        net.minecraft.network.chat.Component component,
        net.minecraft.network.chat.MessageSignature signature,
        net.minecraft.client.multiplayer.chat.GuiMessageSource source,
        net.minecraft.client.multiplayer.chat.GuiMessageTag tag,
        CallbackInfo ci,
        @Local(argsOnly = true) LocalRef<net.minecraft.network.chat.Component> componentRef
    ) {
        if (ANChatUtils.getINSTANCE() != null) {
            componentRef.set(ANChatUtils.getINSTANCE().handleAntiSpam(componentRef.get(), this.trimmedMessages));
        }
    }

    @ModifyExpressionValue(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At(
            value = "NEW",
            target = "(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)Lnet/minecraft/client/multiplayer/chat/GuiMessage;"
        )
    )
    private GuiMessage anpilot$attachSenderToMessage(GuiMessage message) {
        GameProfile sender = ANChatUtils.currentChatSender();
        if (sender == null && ANChatUtils.shouldRenderPlayerHeads()) {
            sender = ANChatUtils.findSenderProfile(message.content().getString());
        }
        ((ANGuiMessageExt) (Object) message).anpilot$setSender(sender);
        return message;
    }

    @ModifyExpressionValue(
        method = "addMessageToDisplayQueue",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/client/multiplayer/chat/GuiMessage;Lnet/minecraft/util/FormattedCharSequence;Z)Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"
        )
    )
    private GuiMessage.Line anpilot$attachSenderToLine(GuiMessage.Line line, @Local(argsOnly = true) GuiMessage message, @Local(ordinal = 1) int lineIndex) {
        GameProfile sender = ((ANGuiMessageExt) (Object) message).anpilot$getSender();
        ANGuiMessageLineExt ext = (ANGuiMessageLineExt) (Object) line;
        ext.anpilot$setSender(sender);
        ext.anpilot$setStartOfEntry(lineIndex == 0);
        return line;
    }

    @ModifyExpressionValue(
        method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;ceil(F)I")
    )
    private int anpilot$expandWidthForHeads(int width) {
        return ANChatUtils.shouldRenderPlayerHeads() ? width + ANChatUtils.chatHeadOffset() : width;
    }
}
