package anpilot.client.minecraft.mixin;

import anpilot.client.features.module.misc.ANChatUtils;
import anpilot.client.minecraft.duck.ANChatGraphicsAccessExt;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public abstract class ANChatComponentLineRenderMixin {
    @Redirect(
        method = "accept(Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z"
        )
    )
    private boolean anpilot$drawHeadAndShiftText(
        ChatComponent.ChatGraphicsAccess graphics,
        int textTop,
        float opacity,
        FormattedCharSequence content,
        GuiMessage.Line line,
        int lineIndex,
        float alpha
    ) {
        if (!ANChatUtils.shouldRenderPlayerHeads() || !(graphics instanceof ANChatGraphicsAccessExt access)) {
            return graphics.handleMessage(textTop, opacity, content);
        }

        ANChatUtils.renderPlayerHead(access.anpilot$getGraphics(), line, textTop, opacity);
        return graphics.handleMessage(textTop, opacity, content);
    }
}
