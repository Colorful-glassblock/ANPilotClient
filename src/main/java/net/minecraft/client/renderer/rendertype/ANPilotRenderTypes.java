package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ANPilotRenderTypes {
    private static final RenderPipeline XRAY_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_xray_lines")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    );

    private static final RenderPipeline XRAY_LINES_VISIBLE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_xray_lines_visible")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build()
    );

    private static final RenderPipeline XRAY_LINES_HIDDEN_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_xray_lines_hidden")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
            .build()
    );

    private static final RenderPipeline XRAY_FILLED_BOX_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_xray_filled_box")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    );

    private static final RenderPipeline TRACER_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_tracer_lines")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withVertexShader(Identifier.fromNamespaceAndPath("anpilotclient", "core/tracer_lines"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("anpilotclient", "core/tracer_lines"))
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    );

    public static final RenderType XRAY_LINES = RenderType.create(
        "anpilot_xray_lines",
        RenderSetup.builder(XRAY_LINES_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()
    );

    public static final RenderType XRAY_LINES_VISIBLE = RenderType.create(
        "anpilot_xray_lines_visible",
        RenderSetup.builder(XRAY_LINES_VISIBLE_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()
    );

    public static final RenderType XRAY_LINES_HIDDEN = RenderType.create(
        "anpilot_xray_lines_hidden",
        RenderSetup.builder(XRAY_LINES_HIDDEN_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()
    );

    public static final RenderType TRACER_LINES = RenderType.create(
        "anpilot_tracer_lines",
        RenderSetup.builder(TRACER_LINES_PIPELINE)
            .createRenderSetup()
    );

    public static final RenderType XRAY_FILLED_BOX = RenderType.create(
        "anpilot_xray_filled_box",
        RenderSetup.builder(XRAY_FILLED_BOX_PIPELINE)
            .sortOnUpload()
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()
    );

    private static final RenderPipeline CHAMS_ENTITY_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_chams_entity")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/entity")
            .withFragmentShader(Identifier.fromNamespaceAndPath("anpilotclient", "core/chams_tint"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    );

    private static final RenderPipeline CHAMS_ENTITY_VISIBLE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_chams_entity_visible")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/entity")
            .withFragmentShader(Identifier.fromNamespaceAndPath("anpilotclient", "core/chams_tint"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build()
    );

    private static final RenderPipeline CHAMS_SHINE_ENTITY_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_chams_shine_entity")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/entity")
            .withFragmentShader(Identifier.fromNamespaceAndPath("anpilotclient", "core/chams_shine"))
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    );

    private static final RenderPipeline CHAMS_SHINE_ENTITY_VISIBLE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_chams_shine_entity_visible")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/entity")
            .withFragmentShader(Identifier.fromNamespaceAndPath("anpilotclient", "core/chams_shine"))
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build()
    );

    private static final RenderPipeline DROPS_ESP_ITEM_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation("pipeline/anpilot_drops_esp_item")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/item")
            .withFragmentShader("core/item")
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build()
    );

    public static RenderType chamsEntity(Identifier texture) {
        return chamsEntity(texture, true);
    }

    public static RenderType chamsEntity(Identifier texture, boolean throughWalls) {
        return RenderType.create(
            throughWalls ? "anpilot_chams_entity" : "anpilot_chams_entity_visible",
            RenderSetup.builder(throughWalls ? CHAMS_ENTITY_PIPELINE : CHAMS_ENTITY_VISIBLE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .sortOnUpload()
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup()
        );
    }

    public static RenderType chamsShineEntity(Identifier texture, boolean throughWalls) {
        return RenderType.create(
            throughWalls ? "anpilot_chams_shine_entity" : "anpilot_chams_shine_entity_visible",
            RenderSetup.builder(throughWalls ? CHAMS_SHINE_ENTITY_PIPELINE : CHAMS_SHINE_ENTITY_VISIBLE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .sortOnUpload()
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup()
        );
    }

    public static RenderType chamsArmorEntity(Identifier texture) {
        return RenderType.create(
            "anpilot_chams_armor_entity",
            RenderSetup.builder(CHAMS_ENTITY_VISIBLE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup()
        );
    }

    public static RenderType chamsShineArmorEntity(Identifier texture) {
        return RenderType.create(
            "anpilot_chams_shine_armor_entity",
            RenderSetup.builder(CHAMS_SHINE_ENTITY_VISIBLE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup()
        );
    }

    public static RenderType dropsEspItem(Identifier texture) {
        return RenderType.create(
            "anpilot_drops_esp_item",
            RenderSetup.builder(DROPS_ESP_ITEM_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .sortOnUpload()
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup()
        );
    }

    private ANPilotRenderTypes() {
    }
}
