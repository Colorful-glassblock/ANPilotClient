package anpilot.client.renderer.render.pipeline

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

object ANRenderPipelines {
    val ROUNDED_RECTANGLE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(id("pipeline/rounded_rect"))
            .withVertexShader(id("core/rounded_rect"))
            .withFragmentShader(id("core/rounded_rect"))
            .withVertexFormat(ANVertexFormats.ROUNDED_RECTANGLE, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val ROUNDED_BORDER_RECTANGLE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(id("pipeline/rounded_border_rect"))
            .withVertexShader(id("core/rounded_border_rect"))
            .withFragmentShader(id("core/rounded_border_rect"))
            .withVertexFormat(ANVertexFormats.ROUNDED_BORDER_RECTANGLE, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val FONT_TEXT: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/font_text"))
            .withVertexShader(id("core/font_text"))
            .withFragmentShader(id("core/font_text"))
            .withSampler("Sampler0")
            .withVertexFormat(ANVertexFormats.FONT_TEXT, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val IMAGE_RECTANGLE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/image_rect"))
            .withVertexShader(id("core/image_rect"))
            .withFragmentShader(id("core/image_rect"))
            .withSampler("Sampler0")
            .withVertexFormat(ANVertexFormats.FONT_TEXT, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val DECOR_IMAGE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/decor_image"))
            .withVertexShader(id("core/decor_image"))
            .withFragmentShader(id("core/decor_image"))
            .withSampler("Sampler0")
            .withVertexFormat(ANVertexFormats.DECOR_IMAGE, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val ROUNDED_IMAGE_RECTANGLE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/rounded_image_rect"))
            .withVertexShader(id("core/rounded_image_rect"))
            .withFragmentShader(id("core/rounded_image_rect"))
            .withSampler("Sampler0")
            .withVertexFormat(ANVertexFormats.ROUNDED_IMAGE_RECTANGLE, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val GRADIENT_RECTANGLE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(id("pipeline/gradient_rect"))
            .withVertexShader(id("core/gradient_rect"))
            .withFragmentShader(id("core/gradient_rect"))
            .withVertexFormat(ANVertexFormats.GRADIENT_RECTANGLE, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val ROUNDED_GRADIENT_RECTANGLE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(id("pipeline/rounded_gradient_rect"))
            .withVertexShader(id("core/rounded_gradient_rect"))
            .withFragmentShader(id("core/rounded_gradient_rect"))
            .withVertexFormat(ANVertexFormats.ROUNDED_GRADIENT_RECTANGLE, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    val BLUR: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/blur"))
            .withVertexShader(id("core/blur"))
            .withFragmentShader(id("core/blur"))
            .withSampler("Sampler0")
            .withVertexFormat(ANVertexFormats.BLUR, VertexFormat.Mode.QUADS)
            .withUsePipelineDrawModeForGui(true)
            .build()
    )

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath("anpilotclient", path)
}
