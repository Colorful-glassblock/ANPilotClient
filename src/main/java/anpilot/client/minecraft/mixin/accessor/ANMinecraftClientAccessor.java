package anpilot.client.minecraft.mixin.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface ANMinecraftClientAccessor {
    @Invoker("startAttack")
    boolean anpilot$startAttack();
}
