package anpilot.client.minecraft.mixin;

import anpilot.client.bootstrap.ANServiceRegistry;
import anpilot.client.features.module.render.ANXRay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class ANSodiumBlockRendererMixin {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int FULL_WHITE = 0xFFFFFFFF;
    private static final float[] FULL_BRIGHTNESS = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

    @Unique
    private boolean anpilot$xrayOpacityBlock;

    @Unique
    private boolean anpilot$xrayFullBrightBlock;

    @Inject(
        method = "renderModel",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void renderModelHook(
        BlockStateModel model,
        BlockState state,
        BlockPos pos,
        BlockPos origin,
        CallbackInfo ci
    ) {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) {
            anpilot$xrayOpacityBlock = false;
            anpilot$xrayFullBrightBlock = false;
            return;
        }

        Object module = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("XRay");
        if (module instanceof ANXRay && ((ANXRay) module).getEnabled()) {
            ANXRay xray = (ANXRay) module;
            boolean visible = xray.isVisible(state.getBlock(), pos);
            anpilot$xrayFullBrightBlock = visible;
            anpilot$xrayOpacityBlock = !visible && xray.isOpacityMode();
            if (!visible && !xray.isOpacityMode()) {
                ci.cancel();
            }
        } else {
            anpilot$xrayOpacityBlock = false;
            anpilot$xrayFullBrightBlock = false;
        }
    }

    @WrapOperation(
        method = "processQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;getRenderType()Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"
        ),
        remap = false
    )
    private ChunkSectionLayer forceTranslucentLayer(MutableQuadViewImpl quad, Operation<ChunkSectionLayer> original) {
        if (anpilot$xrayOpacityBlock) {
            return ChunkSectionLayer.TRANSLUCENT;
        }

        return original.call(quad);
    }

    @WrapOperation(
        method = "processQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;emissive()Z"
        ),
        remap = false
    )
    private boolean forceEmissive(MutableQuadViewImpl quad, Operation<Boolean> original) {
        return anpilot$xrayFullBrightBlock || original.call(quad);
    }

    @WrapOperation(
        method = "processQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V"
        ),
        remap = false
    )
    private void bufferQuadWithFullBrightness(BlockRenderer renderer, MutableQuadViewImpl quad, float[] brightnesses, Material material, Operation<Void> original) {
        if (anpilot$xrayFullBrightBlock) {
            original.call(renderer, quad, FULL_BRIGHTNESS, material);
            return;
        }

        original.call(renderer, quad, brightnesses, material);
    }

    @WrapOperation(
        method = "bufferQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;baseColor(I)I"
        ),
        remap = false
    )
    private int applyXRayOpacity(MutableQuadViewImpl quad, int vertexIndex, Operation<Integer> original) {
        int color = original.call(quad, vertexIndex);
        ANXRay xray = getXRay();
        if (anpilot$xrayFullBrightBlock) {
            return FULL_WHITE;
        }

        if (xray != null && anpilot$xrayOpacityBlock) {
            return xray.applyOpacity(color);
        }

        return color;
    }

    @WrapOperation(
        method = "bufferQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;getLight(I)I"
        ),
        remap = false
    )
    private int applyXRayFullBrightLight(MutableQuadViewImpl quad, int vertexIndex, Operation<Integer> original) {
        if (anpilot$xrayFullBrightBlock) {
            return FULL_BRIGHT;
        }

        return original.call(quad, vertexIndex);
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
