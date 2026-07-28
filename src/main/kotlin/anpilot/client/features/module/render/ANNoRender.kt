package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.setting.ANSetting

class ANNoRender : ANBaseModule(
    name = "NoRender",
    description = "屏蔽反胃、失明迷雾与受击摇晃等干扰视觉的效果",
    category = ANModuleCategory.RENDER,
    chineseName = "禁止渲染"
) {
    val fog = addSetting(ANSetting("Fog", false))
    val darkness = addSetting(ANSetting("Darkness", false))
    val noNausea = addSetting(ANSetting("Nausea", false))
    val noPumpkinOverlay = addSetting(ANSetting("Pumpkin", false))
    val noPortalOverlay = addSetting(ANSetting("Portal", false))
    val noVignette = addSetting(ANSetting("Vignette", false))
    val noSpyglassOverlay = addSetting(ANSetting("Spyglass", false))
    val noPowderSnowOverlay = addSetting(ANSetting("PowderSnow", false))
    val noHurtCamera = addSetting(ANSetting("HurtCam", false))
    val noEatParticles = addSetting(ANSetting("EatParticles", false))
    val noEffectParticle = addSetting(ANSetting("EffectParticle", false))
    val noAmbientParticle = addSetting(ANSetting("AmbientParticle", false))
    val noBreakParticles = addSetting(ANSetting("BreakParticles", false))
    val noFireOverlay = addSetting(ANSetting("FireOverlay", false))
    val noWallOverlay = addSetting(ANSetting("WallOverlay", false))
    val noBossBar = addSetting(ANSetting("BossBar", false))
    val noScoreboard = addSetting(ANSetting("Scoreboard", false))
    val noTitle = addSetting(ANSetting("Title", false))
    val noOverlayMessage = addSetting(ANSetting("OverlayMessage", false))
    val noPotionIcons = addSetting(ANSetting("PotionIcons", false))
    val noCrosshair = addSetting(ANSetting("Crosshair", false))
    val noItemName = addSetting(ANSetting("ItemName", false))
    val noSleepOverlay = addSetting(ANSetting("SleepOverlay", false))
    val noTotemAnimation = addSetting(ANSetting("TotemAnimation", false))
    val noTotemParticle = addSetting(ANSetting("TotemParticle", false))
}
