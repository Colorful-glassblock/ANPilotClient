package anpilot.client.renderer.render

import anpilot.client.renderer.render.state.BlurRenderState
import anpilot.client.renderer.render.state.DecorImageRenderState
import anpilot.client.renderer.render.state.GradientRectangleRenderState
import anpilot.client.renderer.render.state.ImageRectangleRenderState
import anpilot.client.renderer.render.state.RoundedBorderRectangleRenderState
import anpilot.client.renderer.render.state.RoundedGradientRectangleRenderState
import anpilot.client.renderer.render.state.RoundedImageRectangleRenderState
import anpilot.client.renderer.render.state.RoundedRectangleRenderState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.resources.Identifier
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ANRender2DEngine {
    fun blur(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, radius: Float, tintColor: Int, scissorArea: ScreenRectangle? = null) {
        blur(context, x, y, width, height, radius, radius, tintColor, scissorArea)
    }

    fun blur(
        context: GuiGraphicsExtractor,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        cornerRadius: Float,
        blurRadius: Float,
        tintColor: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        if (width <= 0f || height <= 0f || blurRadius <= 0f) {
            return
        }

        context.guiRenderState.addGuiElement(
            BlurRenderState(
                context.pose(),
                x,
                y,
                width,
                height,
                context.guiWidth(),
                context.guiHeight(),
                min(cornerRadius, min(width, height) / 2f),
                max(0f, blurRadius),
                tintColor,
                scissorArea
            )
        )
    }

    fun rect(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, color: Int) {
        context.fill(x.roundToInt(), y.roundToInt(), (x + width).roundToInt(), (y + height).roundToInt(), color)
    }

    fun imageRect(context: GuiGraphicsExtractor, texture: Identifier, x: Float, y: Float, width: Float, height: Float, color: Int, scissorArea: ScreenRectangle? = null) {
        imageRect(context, texture, x, y, width, height, 0f, 0f, 1f, 1f, color, scissorArea)
    }

    fun imageRect(
        context: GuiGraphicsExtractor,
        texture: Identifier,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        u0: Float,
        v0: Float,
        u1: Float,
        v1: Float,
        color: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        if (width <= 0f || height <= 0f) return
        context.guiRenderState.addGuiElement(
            ImageRectangleRenderState(
                context.pose(),
                texture,
                x,
                y,
                width,
                height,
                u0,
                v0,
                u1,
                v1,
                color,
                scissorArea
            )
        )
    }

    fun roundedImageRect(
        context: GuiGraphicsExtractor,
        texture: Identifier,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        if (width <= 0f || height <= 0f) return
        context.guiRenderState.addGuiElement(
            RoundedImageRectangleRenderState(
                context.pose(),
                texture,
                x,
                y,
                width,
                height,
                radius,
                color,
                scissorArea
            )
        )
    }

    fun decorImage(
        context: GuiGraphicsExtractor,
        texture: Identifier,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        color: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        decorImage(context, texture, centerX, centerY, width, height, rotationDegrees, 0f, 0f, 1f, 1f, color, scissorArea)
    }

    fun decorImage(
        context: GuiGraphicsExtractor,
        texture: Identifier,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        u0: Float,
        v0: Float,
        u1: Float,
        v1: Float,
        color: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        if (width <= 0f || height <= 0f) return
        context.guiRenderState.addGuiElement(
            DecorImageRenderState(
                context.pose(),
                texture,
                centerX,
                centerY,
                width,
                height,
                rotationDegrees,
                u0,
                v0,
                u1,
                v1,
                color,
                scissorArea
            )
        )
    }

    fun gradientRect(
        context: GuiGraphicsExtractor,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        topLeftColor: Int,
        topRightColor: Int,
        bottomRightColor: Int,
        bottomLeftColor: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        if (width <= 0f || height <= 0f) return
        context.guiRenderState.addGuiElement(
            GradientRectangleRenderState(
                context.pose(),
                x,
                y,
                width,
                height,
                topLeftColor,
                topRightColor,
                bottomRightColor,
                bottomLeftColor,
                scissorArea
            )
        )
    }

    fun roundedGradientRect(
        context: GuiGraphicsExtractor,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        topLeftColor: Int,
        topRightColor: Int,
        bottomRightColor: Int,
        bottomLeftColor: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        if (width <= 0f || height <= 0f) return
        context.guiRenderState.addGuiElement(
            RoundedGradientRectangleRenderState(
                context.pose(),
                x,
                y,
                width,
                height,
                min(radius, min(width, height) / 2f),
                topLeftColor,
                topRightColor,
                bottomRightColor,
                bottomLeftColor,
                scissorArea
            )
        )
    }

    fun roundedRect(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, radius: Float, color: Int, scissorArea: ScreenRectangle? = null) {
        borderedRoundedRect(context, x, y, width, height, radius, 0f, color, color, scissorArea)
    }

    fun borderedRoundedRect(
        context: GuiGraphicsExtractor,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        borderWidth: Float,
        fillColor: Int,
        borderColor: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        if (width <= 0f || height <= 0f) return
        context.guiRenderState.addGuiElement(
            RoundedBorderRectangleRenderState(
                context.pose(),
                x,
                y,
                width,
                height,
                min(radius, min(width, height) / 2f),
                max(0f, borderWidth),
                fillColor,
                borderColor,
                scissorArea
            )
        )
    }

    fun glowingRoundedRect(
        context: GuiGraphicsExtractor,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Int,
        glowRadius: Float,
        glowColor: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        roundedRectWithGlow(context, x, y, width, height, radius, 0f, color, color, glowRadius, glowColor, scissorArea)
    }

    fun roundedRectWithGlow(
        context: GuiGraphicsExtractor,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        borderWidth: Float,
        fillColor: Int,
        borderColor: Int,
        glowRadius: Float,
        glowColor: Int,
        scissorArea: ScreenRectangle? = null
    ) {
        val clampedRadius = min(radius, min(width, height) / 2f)
        if (clampedRadius <= 0f && glowRadius <= 0f && borderWidth <= 0f) {
            rect(context, x, y, width, height, fillColor)
            return
        }

        context.guiRenderState.addGuiElement(
            RoundedRectangleRenderState(
                context.pose(),
                x,
                y,
                width,
                height,
                clampedRadius,
                max(0f, borderWidth),
                fillColor,
                borderColor,
                max(0f, glowRadius),
                glowColor,
                scissorArea
            )
        )
    }
}
