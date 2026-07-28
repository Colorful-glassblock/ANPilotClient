package anpilot.client.minecraft.mixin;

import anpilot.client.minecraft.duck.ANGuiMessageExt;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public abstract class ANGuiMessageMixin implements ANGuiMessageExt {
    @Unique
    private GameProfile anpilot$sender;

    @Override
    public GameProfile anpilot$getSender() {
        return anpilot$sender;
    }

    @Override
    public void anpilot$setSender(GameProfile sender) {
        this.anpilot$sender = sender;
    }
}
