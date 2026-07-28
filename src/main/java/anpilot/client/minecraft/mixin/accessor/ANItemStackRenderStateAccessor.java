package anpilot.client.minecraft.mixin.accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ANItemStackRenderStateAccessor {
    @Accessor("activeLayerCount")
    int anpilot$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] anpilot$getLayers();
}
