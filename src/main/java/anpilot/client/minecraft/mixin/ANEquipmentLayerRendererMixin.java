package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.renderer.utils.IANEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public abstract class ANEquipmentLayerRendererMixin {
    @Unique
    private static final ThreadLocal<Object> anpilot$currentArmorState = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Identifier> anpilot$currentArmorTexture = new ThreadLocal<>();

    @Inject(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At("HEAD")
    )
    private <S> void anpilot$captureArmorState(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> asset,
        Model<? super S> model,
        S state,
        ItemStack stack,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Identifier playerTexture,
        int outlineColor,
        int order,
        CallbackInfo ci
    ) {
        anpilot$currentArmorState.set(state);
    }

    @Inject(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At("RETURN")
    )
    private <S> void anpilot$clearArmorState(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> asset,
        Model<? super S> model,
        S state,
        ItemStack stack,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Identifier playerTexture,
        int outlineColor,
        int order,
        CallbackInfo ci
    ) {
        anpilot$currentArmorState.remove();
        anpilot$currentArmorTexture.remove();
    }

    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private <S> RenderType anpilot$captureArmorTexture(Identifier texture) {
        anpilot$currentArmorTexture.set(texture);
        return RenderTypes.armorCutoutNoCull(texture);
    }

    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", ordinal = 0)
    )
    private <S> void anpilot$submitArmorChamsLayer(
        OrderedSubmitNodeCollector collector,
        Model<? super S> model,
        S state,
        PoseStack poseStack,
        RenderType renderType,
        int light,
        int overlay,
        int color,
        TextureAtlasSprite sprite,
        int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        collector.submitModel(model, state, poseStack, renderType, light, overlay, color, sprite, outlineColor, crumblingOverlay);

        LivingEntity entity = anpilot$currentEntity();
        if (entity == null || !ANServiceRegistry.INSTANCE.isInitialized()) return;

        var chams = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().chams();
        if (chams != null && chams.getEnabled() && chams.shouldOverlayTexture(entity)) {
            Identifier armorTexture = anpilot$currentArmorTexture.get();
            if (armorTexture == null) return;

            collector.submitModel(
                model,
                state,
                poseStack,
                chams.armorRenderType(armorTexture),
                0x00F000F0,
                overlay,
                chams.colorFor(entity),
                sprite,
                chams.outlineColorFor(entity),
                crumblingOverlay
            );
        }
    }

    @Unique
    private LivingEntity anpilot$currentEntity() {
        Object state = anpilot$currentArmorState.get();
        if (state instanceof IANEntityRenderState renderState && renderState.an$getEntity() instanceof LivingEntity entity) {
            return entity;
        }
        return null;
    }
}
