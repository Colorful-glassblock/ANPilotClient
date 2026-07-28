package anpilot.client.minecraft.mixin;

import anpilot.client.renderer.utils.IANEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class ANEntityRenderStateMixin implements IANEntityRenderState {
    @Unique
    private Entity anpilot$entity;

    @Override
    public Entity an$getEntity() {
        return anpilot$entity;
    }

    @Override
    public void an$setEntity(Entity entity) {
        this.anpilot$entity = entity;
    }
}
