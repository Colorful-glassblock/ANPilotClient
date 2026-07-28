package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth

class ANFreeLook : ANBaseModule(
    name = "FreeLook",
    description = "在第三人称视角下自由旋转观察周围环境而不改变角色朝向",
    category = ANModuleCategory.RENDER,
    chineseName = "自由观察"
) {
    val sensitivity = addSetting(ANSetting("Sensitivity", 8.0f, 0.1f, 10.0f))

    private var previousCameraType: CameraType? = null
    private var yawOffset: Float = 0.0f
    private var pitchOffset: Float = 0.0f

    var cameraYaw: Float = 0.0f
        private set

    var cameraPitch: Float = 0.0f
        private set

    override fun onEnable() {
        val player = mc.player ?: return
        cameraYaw = player.yRot
        cameraPitch = player.xRot
        yawOffset = 0.0f
        pitchOffset = Mth.clamp(player.xRot, -90.0f, 90.0f)
        previousCameraType = mc.options.cameraType

        if (mc.options.cameraType != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK)
        }
    }

    override fun onDisable() {
        val previous = previousCameraType
        if (previous != null && mc.options.cameraType != previous) {
            mc.options.setCameraType(previous)
        }
        previousCameraType = null
    }

    override fun onTick() {
        updateCameraRotation()
    }

    fun cameraMode(): Boolean {
        return enabled
    }

    fun onCameraMouseMove(cursorDeltaX: Double, cursorDeltaY: Double) {
        yawOffset = Mth.clamp(yawOffset + (cursorDeltaX / sensitivity.value).toFloat(), -180.0f, 180.0f)
        pitchOffset = Mth.clamp(pitchOffset + (cursorDeltaY / sensitivity.value).toFloat(), -90.0f, 90.0f)
        updateCameraRotation()
    }

    fun updateCameraRotation() {
        val player = mc.player ?: return
        cameraYaw = Mth.wrapDegrees(player.yRot + yawOffset)
        cameraPitch = pitchOffset
    }
}
