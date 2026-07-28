package anpilot.client.features.module.combat

import anpilot.client.api.module.ANModuleCategory
import anpilot.client.api.module.ANModuleState
import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.manager.ANFriendManager
import anpilot.client.features.module.ANBaseModule
import anpilot.client.features.module.player.ANPacketMine
import anpilot.client.features.setting.ANSetting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class ANAutoMine : ANBaseModule(
    name = "AutoMine",
    description = "自动挖掘敌人附近的方块",
    category = ANModuleCategory.COMBAT,
    chineseName = "自动挖掘",
    defaultState = ANModuleState.DISABLED
) {
    val targetRange = addSetting(ANSetting("TargetRange", 6.0f, 1.0f, 10.0f))
    val delay = addSetting(ANSetting("Delay", 200, 100, 500))

    val friendSync = addSetting(ANSetting("FriendSync", false))
    val fallback = addSetting(ANSetting("Fallback", true) { friendSync.value })

    val antiCrawl = addSetting(ANSetting("AntiCrawl", true))
    val feet = addSetting(ANSetting("Feet", true))
    val head = addSetting(ANSetting("Head", false))
    val ceiling = addSetting(ANSetting("Ceiling", false))
    val floor = addSetting(ANSetting("Floor", false))
    val avoidSelf = addSetting(ANSetting("AvoidSelf", false))

    private var lastMineTime = 0L

    override fun onTick() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        if (player.isCreative || player.isSpectator) return

        val packetMine = packetMineModule() ?: return
        if (!packetMine.enabled || packetMine.isManualMining()) {
            lastMineTime = System.currentTimeMillis()
            return
        }

        val target = findTarget() ?: return
        val now = System.currentTimeMillis()
        if (now - lastMineTime < delay.value) return

        val next = getNextAutoMine(target, packetMine) ?: return
        val state = level.getBlockState(next)
        if (!canMineBlock(state, next) || !canStartMining(packetMine, next)) return

        packetMine.startMining(next, Direction.UP)
        lastMineTime = now
    }

    private fun findTarget(): Player? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null
        val rangeSq = targetRange.value * targetRange.value

        return level.players()
            .asSequence()
            .filter { it !== player && it.isAlive && !it.isSpectator && !it.isCreative }
            .filter { !ANFriendManager.isFriend(it.name.string) }
            .filter { player.distanceToSqr(it) <= rangeSq }
            .minByOrNull { player.distanceToSqr(it) }
    }

    private fun getNextAutoMine(target: Player, packetMine: ANPacketMine): BlockPos? {
        return buildCandidates(target)
            .filter { canConsider(packetMine, it) }
            .sortedWith(
                compareByDescending<MineCandidate> { it.crystalSupport }
                    .thenByDescending { it.layer.priority }
                    .thenBy { squaredDistanceToPlayer(it.pos) }
            )
            .firstOrNull()
            ?.pos
    }

    private fun buildCandidates(target: Player): List<MineCandidate> {
        val base = target.blockPosition()
        val result = linkedSetOf<MineCandidate>()

        if (antiCrawl.value && isCrawling(target)) {
            result += MineCandidate(base.above(), MineLayer.ANTI_CRAWL)
            result += MineCandidate(base.above(2), MineLayer.ANTI_CRAWL)
        }

        if (floor.value) {
            result += MineCandidate(base.below(), MineLayer.FLOOR)
        }

        if (feet.value) {
            result += MineCandidate(base, MineLayer.FEET_INTERSECT)
            horizontalAround(base).forEach { result += MineCandidate(it, MineLayer.FEET) }
        }

        if (head.value) {
            result += MineCandidate(base.above(), MineLayer.BODY_INTERSECT)
            horizontalAround(base.above()).forEach { result += MineCandidate(it, MineLayer.BODY) }
        }

        if (ceiling.value) {
            result += MineCandidate(base.above(2), MineLayer.CEILING)
        }

        return result.map { it.copy(crystalSupport = hasCrystalSupport(it.pos)) }
    }

    private fun canConsider(packetMine: ANPacketMine, candidate: MineCandidate): Boolean {
        val pos = candidate.pos
        val level = mc.level ?: return false
        val player = mc.player ?: return false
        if (!canMineBlock(level.getBlockState(pos), pos)) return false
        if (packetMine.isMining(pos)) return false
        if (player.eyePosition.distanceToSqr(Vec3.atCenterOf(pos)) > packetMine.getMiningRange() * packetMine.getMiningRange()) {
            return false
        }
        if (avoidSelf.value && player.boundingBox.inflate(0.25).intersects(AABB(pos))) return false
        if (candidate.layer.requiresCrystalSupport && !candidate.crystalSupport) return false
        if (friendSync.value && isSyncedWithFriend(pos)) return false
        return canStartMining(packetMine, pos)
    }

    private fun canStartMining(packetMine: ANPacketMine, pos: BlockPos): Boolean {
        if (packetMine.isMining(pos)) return false
        if (packetMine.hasFreeMine()) return true

        val mainReady = packetMine.isMainDoneMining() || packetMine.hasMainMinedFor(30)
        val packetReady = packetMine.isPacketBlockMined() || packetMine.hasPacketMinedFor(30)
        return mainReady && packetReady
    }

    private fun canMineBlock(state: BlockState, pos: BlockPos): Boolean {
        val level = mc.level ?: return false
        return !state.isAir && state.fluidState.isEmpty && state.getDestroySpeed(level, pos) != -1.0f
    }

    private fun isSyncedWithFriend(pos: BlockPos): Boolean {
        val player = mc.player ?: return false
        val level = mc.level ?: return false
        val fallbackEnabled = fallback.value
        val nearFriend = level.players()
            .asSequence()
            .filter { it !== player && ANFriendManager.isFriend(it.name.string) }
            .any { it.distanceToSqr(Vec3.atCenterOf(pos)) <= 9.0 }

        return nearFriend && !fallbackEnabled
    }

    private fun hasCrystalSupport(pos: BlockPos): Boolean {
        val level = mc.level ?: return false
        val below = level.getBlockState(pos.below())
        return below.`is`(Blocks.OBSIDIAN) || below.`is`(Blocks.BEDROCK)
    }

    private fun squaredDistanceToPlayer(pos: BlockPos): Double {
        val player = mc.player ?: return Double.MAX_VALUE
        return player.eyePosition.distanceToSqr(Vec3.atCenterOf(pos))
    }

    private fun horizontalAround(pos: BlockPos): List<BlockPos> {
        return listOf(
            pos.relative(Direction.NORTH),
            pos.relative(Direction.SOUTH),
            pos.relative(Direction.EAST),
            pos.relative(Direction.WEST)
        )
    }

    private fun isCrawling(player: Player): Boolean {
        return player.pose == Pose.SWIMMING || player.boundingBox.ysize < 1.2
    }

    private fun packetMineModule(): ANPacketMine? {
        if (!ANServiceRegistry.isInitialized) return null
        return ANServiceRegistry.runtime.moduleManager.get("PacketMine") as? ANPacketMine
    }

    private data class MineCandidate(
        val pos: BlockPos,
        val layer: MineLayer,
        val crystalSupport: Boolean = false
    )

    private enum class MineLayer(
        val priority: Int,
        val requiresCrystalSupport: Boolean = false
    ) {
        ANTI_CRAWL(7),
        FEET_INTERSECT(6, requiresCrystalSupport = true),
        FLOOR(5),
        BODY_INTERSECT(4),
        FEET(3, requiresCrystalSupport = true),
        BODY(2),
        CEILING(1)
    }
}
