package anpilot.client.minecraft.mixin;

import anpilot.client.minecraft.duck.ANGuiMessageLineExt;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
public abstract class ANGuiMessageLineMixin implements ANGuiMessageLineExt {
    @Unique
    private GameProfile anpilot$sender;
    @Unique
    private boolean anpilot$startOfEntry;

    @Override
    public GameProfile anpilot$getSender() {
        return anpilot$sender;
    }

    @Override
    public void anpilot$setSender(GameProfile sender) {
        this.anpilot$sender = sender;
    }

    @Override
    public boolean anpilot$isStartOfEntry() {
        return anpilot$startOfEntry;
    }

    @Override
    public void anpilot$setStartOfEntry(boolean startOfEntry) {
        this.anpilot$startOfEntry = startOfEntry;
    }
}
