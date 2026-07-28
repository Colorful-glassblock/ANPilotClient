package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.ANTickEvent
import anpilot.client.features.event.impl.RenderAfterWorldEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.utility.ANTimer
import anpilot.client.minecraft.mixin.accessor.ANMinecraftClientAccessor
import net.minecraft.client.Minecraft

class ANAntiAFK : ANBaseModule(
    name = "AntiAFK",
    description = "模拟物理输入定期转向、攻击与跳跃，防止挂机被踢出服务器",
    category = ANModuleCategory.MISC,
    chineseName = "反挂机"
) {
    val rotate = addSetting(ANSetting("Rotate", true))
    val rotateSpeed = addSetting(ANSetting("RotateSpeed", 1.0f, 0.1f, 10.0f))

    val punch = addSetting(ANSetting("Attack", true))
    val punchDelay = addSetting(ANSetting("AttackDelay", 2.0f, 1.0f, 60.0f))

    val jump = addSetting(ANSetting("Jump", true))
    val jumpDelay = addSetting(ANSetting("JumpDelay", 10.0f, 1.0f, 60.0f))

    private val punchTimer = ANTimer()
    private val jumpTimer = ANTimer()

    private var jumpActive = false
    private var jumpActiveTicks = 0
    private var lastFrameTime = 0L

    override fun onEnable() {
        punchTimer.reset()
        jumpTimer.reset()
        jumpActive = false
        jumpActiveTicks = 0
        lastFrameTime = 0L
    }

    override fun onDisable() {
        if (jumpActive) {
            mc.options.keyJump.isDown = false
        }
    }

    @ANEventHandler
    fun onRenderAfterWorld(event: RenderAfterWorldEvent) {
        val player = mc.player ?: return
        if (mc.level == null) return

        val currentTime = System.nanoTime()
        if (lastFrameTime == 0L) {
            lastFrameTime = currentTime
            return
        }

        var deltaTimeSeconds = (currentTime - lastFrameTime) / 1_000_000_000.0
        lastFrameTime = currentTime

        if (deltaTimeSeconds > 0.1) {
            deltaTimeSeconds = 0.1
        }

        if (rotate.value) {
            val speed = rotateSpeed.value.toDouble() 
            val degreesPerSecond = speed * 20.0
            val degreesThisFrame = degreesPerSecond * deltaTimeSeconds
            player.turn(degreesThisFrame / 0.15, 0.0)
        }
    }

    @ANEventHandler
    fun onTick(event: ANTickEvent) {
        val player = mc.player ?: return
        if (mc.level == null) return

        
        if (punch.value) {
            val delayMs = (punchDelay.value * 1000).toLong()
            if (punchTimer.passedAndResetMs(delayMs)) {
                
                (mc as? ANMinecraftClientAccessor)?.`anpilot$startAttack`()
            }
        }

        
        if (jump.value) {
            val delayMs = (jumpDelay.value * 1000).toLong()
            if (jumpTimer.passedAndResetMs(delayMs)) {
                if (player.onGround() && !jumpActive) {
                    mc.options.keyJump.isDown = true
                    jumpActive = true
                    jumpActiveTicks = 0
                }
            }
        }

        if (jumpActive) {
            jumpActiveTicks++
            if (jumpActiveTicks >= 2) {
                mc.options.keyJump.isDown = false
                jumpActive = false
            }
        }
    }
}
