package anpilot.client.features.module.misc

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.ANTickEvent
import anpilot.client.features.event.impl.PacketEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items

class ANAutoFish : ANBaseModule(
    name = "AutoFish",
    description = "自动收线拉杆与抛竿钓鱼",
    category = ANModuleCategory.MISC,
    chineseName = "自动钓鱼"
) {
    val castDelay = addSetting(ANSetting("CastDelay", 15f, 10f, 25f))

    private var autoReel = false
    private var autoReelTicks = 0
    private var autoCastTicks = 0

    override fun onDisable() {
        autoReel = false
        autoReelTicks = 0
        autoCastTicks = 0
    }

    @ANEventHandler
    fun onPacketReceive(event: PacketEvent.Receive) {
        val player = mc.player ?: return
        if (mc.level == null || !holdingFishingRod()) return

        val packet = event.packet as? ClientboundSoundPacket ?: return
        if (packet.sound.value() != SoundEvents.FISHING_BOBBER_SPLASH) return

        val fishHook = player.fishing ?: return
        if (fishHook.playerOwner !== player) return

        val distSq = fishHook.distanceToSqr(packet.x, packet.y, packet.z)
        if (distSq <= MAX_SOUND_DISTANCE * MAX_SOUND_DISTANCE) {
            autoReel = true
            autoReelTicks = REEL_DELAY_TICKS
        }
    }

    @ANEventHandler
    fun onTick(event: ANTickEvent) {
        val player = mc.player ?: return clear()
        if (mc.level == null || !holdingFishingRod()) return clear()

        val fishHook = player.fishing
        if ((fishHook == null || fishHook.hookedIn != null) && autoCastTicks <= 0) {
            useFishingRod()
            autoCastTicks = castDelay.value.toInt()
            return
        }

        if (autoReel) {
            if (autoReelTicks <= 0) {
                useFishingRod()
                autoReel = false
                return
            }
            autoReelTicks--
        }

        autoCastTicks--
    }

    private fun useFishingRod() {
        val player = mc.player ?: return
        mc.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
        player.swing(InteractionHand.MAIN_HAND)
    }

    private fun holdingFishingRod(): Boolean {
        val player = mc.player ?: return false
        return player.mainHandItem.`is`(Items.FISHING_ROD)
    }

    private fun clear() {
        autoReel = false
        autoReelTicks = 0
        autoCastTicks = 0
    }

    private companion object {
        private const val REEL_DELAY_TICKS = 4
        private const val MAX_SOUND_DISTANCE = 2.0
    }
}
