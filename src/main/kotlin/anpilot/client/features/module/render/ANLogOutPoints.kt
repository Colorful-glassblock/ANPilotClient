package anpilot.client.features.module.render

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.features.event.ANEventHandler
import anpilot.client.features.event.impl.Render2DEvent
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.ANWorldRenderModule
import anpilot.client.features.setting.ANSetting
import anpilot.client.features.setting.impl.ColorGroupSetting
import anpilot.client.renderer.ANColor
import anpilot.client.renderer.font.ANFontRenderer
import anpilot.client.renderer.render.ANRender2DEngine
import anpilot.client.renderer.render.ANRender3DEngine
import com.mojang.authlib.GameProfile
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

class ANLogOutPoints : ANBaseModule(
    name = "LogoutSpots",
    description = "在其他玩家下线断开连接的位置保留绘制3D选框与玩家残像模型",
    category = ANModuleCategory.RENDER,
    chineseName = "下线位置"
), ANWorldRenderModule {
    enum class RenderMode {
        Box, Model, Both
    }

    val scale = addSetting(ANSetting("Scale", 1.0f, 0.1f, 2.0f))
    val renderMode = addSetting(ANSetting("RenderMode", RenderMode.Both))

    val lineColor = addSetting(ANSetting("LineColor", ColorGroupSetting(Color(255, 0, 255, 255).rgb)))
    val sideColor = addSetting(ANSetting("SideColor", ColorGroupSetting(Color(255, 0, 255, 55).rgb)))
    val textColor = addSetting(ANSetting("TextColor", ColorGroupSetting(Color(255, 255, 255, 255).rgb)))
    val plateFill = addSetting(ANSetting("PlateFill", ColorGroupSetting(Color(0, 0, 0, 120).rgb)))
    val plateBorder = addSetting(ANSetting("PlateBorder", ColorGroupSetting(Color(0, 0, 0, 0).rgb)))

    private val logoutSpots = ArrayList<LogoutSpot>()
    
    private val lastOnlineUUIDs = HashSet<UUID>()
    private val lastKnownStates = HashMap<UUID, LastKnownPlayerState>()
    private var lastDimension: ResourceKey<Level>? = null
    private var ticks = 0
    
    private var fontRenderer: ANFontRenderer? = null
    private var lastRenderMode: RenderMode? = null

    override fun onEnable() {
        logoutSpots.clear()
        lastOnlineUUIDs.clear()
        lastKnownStates.clear()
        lastDimension = null
        lastRenderMode = renderMode.value
        ticks = 0
    }

    override fun onDisable() {
        for (spot in logoutSpots) {
            spot.entity?.despawnPlayer()
        }
        logoutSpots.clear()
        lastOnlineUUIDs.clear()
        lastKnownStates.clear()
    }

    override fun onTick() {
        ticks++
        if (fullNullCheck()) {
            if (logoutSpots.isNotEmpty() || lastOnlineUUIDs.isNotEmpty() || lastKnownStates.isNotEmpty()) {
                for (spot in logoutSpots) {
                    spot.entity?.despawnPlayer()
                }
                logoutSpots.clear()
                lastOnlineUUIDs.clear()
                lastKnownStates.clear()
                lastDimension = null
            }
            return
        }
        val connection = mc.connection ?: return
        val level = mc.level ?: return

        
        val currentDimension = level.dimension()
        if (currentDimension != lastDimension) {
            for (spot in logoutSpots) {
                spot.entity?.despawnPlayer()
            }
            logoutSpots.clear()
            lastOnlineUUIDs.clear()
            lastKnownStates.clear()
        }
        lastDimension = currentDimension

        
        if (renderMode.value != lastRenderMode) {
            refreshEntities()
            lastRenderMode = renderMode.value
        }

        val currentOnline = connection.onlinePlayers
        val currentUUIDs = currentOnline.map { it.profile.id }.toSet()

        
        for (lastUuid in lastOnlineUUIDs) {
            if (!currentUUIDs.contains(lastUuid)) {
                val state = lastKnownStates[lastUuid]
                if (state != null) {
                    val ticksSinceLastSeen = ticks - state.tickLastSeen
                    if (ticksSinceLastSeen <= 5) {
                        addSpot(state.player)
                    }
                    lastKnownStates.remove(lastUuid)
                }
            }
        }

        
        for (uuid in currentUUIDs) {
            val toRemove = logoutSpots.filter { it.uuid == uuid }
            for (spot in toRemove) {
                spot.entity?.despawnPlayer()
            }
            logoutSpots.removeIf { it.uuid == uuid }
        }

        
        lastOnlineUUIDs.clear()
        lastOnlineUUIDs.addAll(currentUUIDs)

        
        for (player in level.players()) {
            if (player != mc.player) {
                lastKnownStates[player.uuid] = LastKnownPlayerState(player, ticks)
            }
        }
    }

    override fun renderWorld(context: LevelRenderContext) {
        if (renderMode.value == RenderMode.Model) return
        val line = lineColor.value.toANColor()
        val side = sideColor.value.toANColor()

        for (spot in logoutSpots) {
            val box = AABB(
                spot.x, spot.y, spot.z,
                spot.x + spot.width, spot.y + spot.height, spot.z + spot.width
            )
            ANRender3DEngine.box(context, box, line, side)
        }
    }

    @ANEventHandler
    fun onRender2D(event: Render2DEvent) {
        val localPlayer = mc.player ?: return
        val customFont = fontRenderer ?: ANFontRenderer(mc.font).also { fontRenderer = it }
        val context = event.context

        for (spot in logoutSpots) {
            val worldPos = Vec3(
                spot.x + spot.width / 2.0,
                spot.y + spot.height + 0.5,
                spot.z + spot.width / 2.0
            )

            val distance = worldPos.distanceTo(localPlayer.eyePosition)
            val distFactor = 12f / maxOf(distance.toFloat(), 5f)
            val rawScale = 0.5f * distFactor
            val uiScale = (rawScale * scale.value).coerceIn(0.1f, 2.0f)
            val scaleFactor = uiScale / 0.5f

            val screen = mc.gameRenderer.projectPointToScreen(worldPos)
            if (screen.z < 0f || screen.z > 1f) continue

            val x = ((screen.x + 1.0) * 0.5 * context.guiWidth()).toFloat()
            val y = ((1.0 - screen.y) * 0.5 * context.guiHeight()).toFloat()
            if (x.isNaN() || y.isNaN()) continue

            val healthText = " ${spot.health}"
            val finalString = "${spot.name}$healthText"
            val nameWidth = customFont.width(finalString, uiScale)

            val rectWidth = nameWidth + 10f * scaleFactor
            val rectHeight = 10f * scaleFactor
            val rectX = x - rectWidth / 2f
            val gap = 4f * scaleFactor
            val rectY = y - gap - rectHeight

            
            ANRender2DEngine.borderedRoundedRect(
                context,
                rectX,
                rectY,
                rectWidth,
                rectHeight,
                7f * scaleFactor,
                1f * scaleFactor,
                plateFill.value.getColorRGB().rgb,
                plateBorder.value.getColorRGB().rgb
            )

            
            val healthPercentage = spot.health.toDouble() / spot.maxHealth
            val healthColor = when {
                healthPercentage <= 0.333 -> Color(225, 25, 25).rgb
                healthPercentage <= 0.666 -> Color(225, 105, 25).rgb
                else -> Color(25, 225, 25).rgb
            }

            
            val textX = x - nameWidth / 2f
            val textY = rectY + 2f * scaleFactor

            customFont.draw(context, spot.name, textX, textY, textColor.value.getColorRGB().rgb, uiScale)
            customFont.draw(
                context,
                healthText,
                textX + customFont.width(spot.name, uiScale),
                textY,
                healthColor,
                uiScale
            )
        }
    }

    private fun addSpot(player: Player) {
        val halfWidth = player.bbWidth / 2.0
        val spot = LogoutSpot(
            uuid = player.uuid,
            name = player.name.string,
            x = player.x - halfWidth,
            y = player.y,
            z = player.z - halfWidth,
            width = player.bbWidth.toDouble(),
            height = player.bbHeight.toDouble(),
            health = (player.health + player.absorptionAmount).roundToInt(),
            maxHealth = (player.maxHealth + player.absorptionAmount).roundToInt(),
            yaw = player.yRot,
            pitch = player.xRot,
            yawHead = player.yHeadRot,
            yawBody = player.yBodyRot,
            modelParts = LogoutPlayerEntity.getModelParts(player)
        )
        
        logoutSpots.removeIf { 
            if (it.uuid == spot.uuid) {
                it.entity?.despawnPlayer()
                true
            } else {
                false
            }
        }

        if (renderMode.value == RenderMode.Model || renderMode.value == RenderMode.Both) {
            val level = mc.level
            if (level != null) {
                val profile = GameProfile(player.uuid, player.name.string)
                val entity = LogoutPlayerEntity(level, profile, player)
                entity.spawnPlayer()
                spot.entity = entity
            }
        }

        logoutSpots.add(spot)
    }

    private fun refreshEntities() {
        val level = mc.level ?: return
        for (spot in logoutSpots) {
            spot.entity?.despawnPlayer()
            spot.entity = null

            if (renderMode.value == RenderMode.Model || renderMode.value == RenderMode.Both) {
                val profile = GameProfile(spot.uuid, spot.name)
                val entity = LogoutPlayerEntity(level, profile, spot)
                entity.spawnPlayer()
                spot.entity = entity
            }
        }
    }

    private fun ColorGroupSetting.toANColor(): ANColor = ANColor.fromArgb(getColor())

    private data class LastKnownPlayerState(
        val player: Player,
        val tickLastSeen: Int
    )

    private data class LogoutSpot(
        val uuid: UUID,
        val name: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val width: Double,
        val height: Double,
        val health: Int,
        val maxHealth: Int,
        val yaw: Float,
        val pitch: Float,
        val yawHead: Float,
        val yawBody: Float,
        val modelParts: Byte,
        var entity: LogoutPlayerEntity? = null
    )

    private class LogoutPlayerEntity : RemotePlayer {
        constructor(level: ClientLevel, profile: GameProfile, player: Player) : super(level, profile) {
            setPos(player.x, player.y, player.z)
            yRot = player.yRot
            xRot = player.xRot
            yHeadRot = player.yHeadRot
            yBodyRot = player.yBodyRot
            attackAnim = player.attackAnim

            val modelParts = player.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION)
            entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, modelParts)

            attributes.assignAllValues(player.attributes)

            isShiftKeyDown = player.isShiftKeyDown
            isSwimming = player.isSwimming
            setPose(player.pose)
            health = player.health

            inventory.replaceWith(player.inventory)
            id = CURRENT_ID.incrementAndGet()
        }

        constructor(level: ClientLevel, profile: GameProfile, spot: LogoutSpot) : super(level, profile) {
            setPos(spot.x + spot.width / 2.0, spot.y, spot.z + spot.width / 2.0)
            yRot = spot.yaw
            xRot = spot.pitch
            yHeadRot = spot.yawHead
            yBodyRot = spot.yawBody

            entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, spot.modelParts)

            health = spot.health.toFloat()
            id = CURRENT_ID.incrementAndGet()
        }

        override fun isAlive(): Boolean = true
        override fun shouldShowName(): Boolean = false
        override fun isCustomNameVisible(): Boolean = false

        fun spawnPlayer() {
            unsetRemoved()
            Minecraft.getInstance().level?.addEntity(this)
        }

        fun despawnPlayer() {
            Minecraft.getInstance().level?.removeEntity(id, RemovalReason.DISCARDED)
            setRemoved(RemovalReason.DISCARDED)
        }

        companion object {
            val CURRENT_ID = AtomicInteger(2000000)
            
            fun getModelParts(player: Player): Byte {
                return player.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION)
            }
        }
    }
}
