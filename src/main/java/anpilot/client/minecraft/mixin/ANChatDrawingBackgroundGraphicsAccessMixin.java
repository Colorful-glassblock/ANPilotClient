package anpilot.client.minecraft.mixin;

import anpilot.client.minecraft.duck.ANChatGraphicsAccessExt;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public abstract class ANChatDrawingBackgroundGraphicsAccessMixin implements ANChatGraphicsAccessExt {
    @Shadow
    @Final
    private GuiGraphicsExtractor graphics;

    @Override
    public GuiGraphicsExtractor anpilot$getGraphics() {
        return graphics;
    }
}
