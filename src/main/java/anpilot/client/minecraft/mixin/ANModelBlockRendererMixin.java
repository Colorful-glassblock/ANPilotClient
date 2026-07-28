package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANXRay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.class)
public abstract class ANModelBlockRendererMixin {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int FULL_WHITE = 0xFFFFFFFF;

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private void shouldRenderFaceHook(BlockAndTintGetter level, BlockState state, Direction face, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ANXRay xray = getXRay();
        if (xray == null) return;

        BlockPos blockPos = pos.relative(face.getOpposite());
        if (xray.isVisible(state.getBlock(), blockPos)) {
            Block neighborBlock = level.getBlockState(pos).getBlock();
            cir.setReturnValue(!xray.isVisible(neighborBlock, pos));
        } else if (!xray.isOpacityMode()) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
        method = "putQuadWithTint",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
        )
    )
    private void putQuadWithXRayOpacity(
        BlockQuadOutput output,
        float x,
        float y,
        float z,
        BakedQuad quad,
        QuadInstance quadInstance,
        Operation<Void> original,
        BlockQuadOutput methodOutput,
        float red,
        float green,
        float blue,
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        BakedQuad methodQuad
    ) {
        ANXRay xray = getXRay();
        if (xray != null) {
            boolean visible = xray.isVisible(state.getBlock(), pos);
            if (visible) {
                for (int i = 0; i < 4; i++) {
                    quadInstance.setLightCoords(i, FULL_BRIGHT);
                    quadInstance.setColor(i, FULL_WHITE);
                }
            } else if (xray.isOpacityMode()) {
                for (int i = 0; i < 4; i++) {
                    quadInstance.setColor(i, xray.applyOpacity(quadInstance.getColor(i)));
                }
                quad = withTranslucentLayer(quad);
            }
        }

        original.call(output, x, y, z, quad, quadInstance);
    }

    private static BakedQuad withTranslucentLayer(BakedQuad quad) {
        BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
        if (materialInfo.layer() == ChunkSectionLayer.TRANSLUCENT) return quad;

        BakedQuad.MaterialInfo translucentMaterial = new BakedQuad.MaterialInfo(
            materialInfo.sprite(),
            ChunkSectionLayer.TRANSLUCENT,
            materialInfo.itemRenderType(),
            materialInfo.tintIndex(),
            materialInfo.shade(),
            materialInfo.lightEmission()
        );

        return new BakedQuad(
            quad.position0(),
            quad.position1(),
            quad.position2(),
            quad.position3(),
            quad.packedUV0(),
            quad.packedUV1(),
            quad.packedUV2(),
            quad.packedUV3(),
            quad.direction(),
            translucentMaterial
        );
    }

    private static ANXRay getXRay() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return null;

        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("XRay");
        if (module instanceof ANXRay xray && xray.getEnabled()) {
            return xray;
        }

        return null;
    }
}
