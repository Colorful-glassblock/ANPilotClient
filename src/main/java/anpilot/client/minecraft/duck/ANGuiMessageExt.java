package anpilot.client.minecraft.duck;

import com.mojang.authlib.GameProfile;

public interface ANGuiMessageExt {
    GameProfile anpilot$getSender();

    void anpilot$setSender(GameProfile sender);
}
