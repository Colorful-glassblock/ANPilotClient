package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting

class ANViewModel : ANBaseModule(
    name = "ViewModel",
    description = "自定义调整第一人称手持物品与手臂的位置偏移、旋转角度与外观缩放",
    category = ANModuleCategory.RENDER,
    chineseName = "手部模型"
) {
    val positionX = addSetting(ANSetting("PositionX", 0.0f, -3.0f, 3.0f))
    val positionY = addSetting(ANSetting("PositionY", 0.0f, -3.0f, 3.0f))
    val positionZ = addSetting(ANSetting("PositionZ", 0.0f, -3.0f, 3.0f))
    val scale = addSetting(ANSetting("Scale", 1.0f, 0.1f, 2.0f))

    val rotation = addSetting(ANSetting("Rotation", true))
    val rotationX = addSetting(ANSetting("RotationX", 0.0f, -180.0f, 180.0f) { rotation.value })
    val rotationY = addSetting(ANSetting("RotationY", 0.0f, -180.0f, 180.0f) { rotation.value })
    val rotationZ = addSetting(ANSetting("RotationZ", 0.0f, -180.0f, 180.0f) { rotation.value })

    val animate = addSetting(ANSetting("Animate", true))
    val animateX = addSetting(ANSetting("AnimateX", false) { animate.value })
    val animateY = addSetting(ANSetting("AnimateY", false) { animate.value })
    val animateZ = addSetting(ANSetting("AnimateZ", false) { animate.value })
    val speedAnimate = addSetting(ANSetting("SpeedAnimate", 1.0f, 1.0f, 5.0f) { animate.value })

    val eat = addSetting(ANSetting("Eat", true))
    val eatX = addSetting(ANSetting("EatX", 1.0f, -1.0f, 2.0f) { eat.value })
    val eatY = addSetting(ANSetting("EatY", 1.0f, -1.0f, 2.0f) { eat.value })

    private var prevX = 0.0f
    private var prevY = 0.0f
    private var prevZ = 0.0f

    override fun onTick() {
        prevX = rotationX.value
        prevY = rotationY.value
        prevZ = rotationZ.value

        if (animateX.value) rotationX.setValue(rotate(rotationX.value, speedAnimate.value))
        if (animateY.value) rotationY.setValue(rotate(rotationY.value, speedAnimate.value))
        if (animateZ.value) rotationZ.setValue(rotate(rotationZ.value, speedAnimate.value))
    }

    fun interpolatedX(frameInterp: Float): Float = interpolate(prevX, rotationX.value, frameInterp)
    fun interpolatedY(frameInterp: Float): Float = interpolate(prevY, rotationY.value, frameInterp)
    fun interpolatedZ(frameInterp: Float): Float = interpolate(prevZ, rotationZ.value, frameInterp)

    private fun rotate(value: Float, speed: Float): Float {
        val next = value - speed
        return if (next <= 180.0f && next > -180.0f) next else 180.0f
    }

    private fun interpolate(previous: Float, current: Float, frameInterp: Float): Float {
        return previous + (current - previous) * frameInterp
    }
}
