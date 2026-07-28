package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANNoBobView;
import anpilot.client.features.module.render.ANViewModel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ANItemInHandRendererMixin {
    @ModifyArg(
            method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;"),
            index = 0
    )
    private float onRenderHandsBobRotation(float angle) {
        return angle * anpilot$handBobAmount();
    }

    @WrapOperation(
            method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V")
    )
    private void onRenderArmWithItemCall(ItemInHandRenderer renderer, AbstractClientPlayer player, float frameInterp,
                                         float xRot, InteractionHand hand, float attack, ItemStack itemStack,
                                         float inverseArmHeight, PoseStack poseStack,
                                         SubmitNodeCollector submitNodeCollector, int lightCoords,
                                         Operation<Void> original) {
        float amount = anpilot$handBobAmount();
        original.call(renderer, player, frameInterp, xRot, hand, attack * amount, itemStack,
                inverseArmHeight * amount, poseStack, submitNodeCollector, lightCoords);
    }

    @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void onRenderArmWithItem(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack,
                                     ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        ANViewModel viewModel = anpilot$viewModel();
        if (viewModel != null && viewModel.getEnabled()) {
            anpilot$applyViewModel(viewModel, frameInterp, poseStack);
        }
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD")
    )
    private void onRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                              PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                              CallbackInfo ci) {
        anpilot$applyLocalScale(poseStack);
    }

    @Inject(
            method = "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V",
            at = @At("HEAD")
    )
    private void onRenderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                                   float armHeight, float attackProgress, HumanoidArm arm, CallbackInfo ci) {
        anpilot$applyLocalScale(poseStack);
    }

    @ModifyConstant(
            method = "applyEatTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
            constant = @Constant(floatValue = 0.6f),
            require = 0
    )
    private float onEatXOffset(float original) {
        ANViewModel viewModel = anpilot$viewModel();
        if (viewModel == null || !viewModel.getEnabled() || !viewModel.getEat().getValue()) return original;
        return original * viewModel.getEatX().getValue();
    }

    @ModifyConstant(
            method = "applyEatTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
            constant = @Constant(floatValue = -0.5f)
    )
    private float onEatYOffset(float original) {
        ANViewModel viewModel = anpilot$viewModel();
        if (viewModel == null || !viewModel.getEnabled() || !viewModel.getEat().getValue()) return original;
        return original * viewModel.getEatY().getValue();
    }

    @Unique
    private static void anpilot$applyViewModel(ANViewModel viewModel, float frameInterp, PoseStack poseStack) {
        poseStack.translate(viewModel.getPositionX().getValue(), viewModel.getPositionY().getValue(), viewModel.getPositionZ().getValue());
        if (viewModel.getRotation().getValue()) {
            anpilot$rotate(poseStack, viewModel.interpolatedX(frameInterp), viewModel.interpolatedY(frameInterp), viewModel.interpolatedZ(frameInterp));
        }
    }

    @Unique
    private static void anpilot$applyLocalScale(PoseStack poseStack) {
        ANViewModel viewModel = anpilot$viewModel();
        if (viewModel == null || !viewModel.getEnabled()) return;
        float scale = viewModel.getScale().getValue();
        poseStack.scale(scale, scale, scale);
    }

    @Unique
    private static void anpilot$rotate(PoseStack poseStack, float x, float y, float z) {
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(x)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(y)));
        poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(z)));
    }

    @Unique
    private static ANViewModel anpilot$viewModel() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return null;
        return ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().viewModel();
    }

    @Unique
    private static float anpilot$handBobAmount() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return 1.0F;

        ANNoBobView noBobView = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().noBobView();
        if (noBobView == null || !noBobView.getEnabled()) return 1.0F;

        float amount = noBobView.getHandBob().getValue();
        if (amount < 0.0F) return 0.0F;
        if (amount > 1.0F) return 1.0F;
        return amount;
    }
}
