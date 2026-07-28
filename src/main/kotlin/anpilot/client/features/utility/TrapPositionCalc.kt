package anpilot.client.features.utility

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import java.util.*
import java.util.concurrent.ConcurrentNavigableMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.stream.Collectors

enum class TrapLayer {
    FEET, BODY, CEILING, FLOOR, EXTEND_FEET, EXTEND_BODY, FEET_INTERSECT, BODY_INTERSECT
}

data class TrapSpec(
    val layers: EnumSet<TrapLayer>,
    val extendFeet: Boolean = false,
    val extendBody: Boolean = false
)





class TrapPositionCalc(
    private val trapOrdering: Comparator<BlockPos> = Comparator.comparingInt<BlockPos> { it.y }
        .thenComparingInt { it.x }
        .thenComparingInt { it.z }
) {
    val trapPositions: ConcurrentNavigableMap<BlockPos, TrapLayer> = ConcurrentSkipListMap(trapOrdering)

    fun calcTrap(boundingBox: AABB, trapSpec: TrapSpec) {
        trapPositions.clear()

        val origin: MutableSet<BlockPos> = BlockPos.betweenClosedStream(boundingBox)
            .map { it.immutable() }
            .collect(Collectors.toCollection { HashSet() })

        val feetY = boundingBox.minY.toInt()
        val bodyY = feetY + 1

        for (blockPos in origin) {
            addCoreLayers(trapSpec.layers, origin, blockPos, feetY, bodyY)
        }

        extendLayers(trapSpec, origin, feetY, bodyY)
    }

    private fun addCoreLayers(
        layers: EnumSet<TrapLayer>,
        origin: Set<BlockPos>,
        blockPos: BlockPos,
        feetY: Int,
        bodyY: Int
    ) {
        if (layers.contains(TrapLayer.FEET_INTERSECT) || layers.contains(TrapLayer.BODY_INTERSECT)) {
            for (pos in origin) {
                val inY = pos.y
                if (feetY == inY) {
                    trapPositions[pos] = TrapLayer.FEET_INTERSECT
                } else if (bodyY == inY) {
                    trapPositions[pos] = TrapLayer.BODY_INTERSECT
                }
            }
        }

        val y = blockPos.y
        if (feetY == y) {
            if (layers.contains(TrapLayer.FEET)) {
                extendTrapAroundPos(blockPos, origin, TrapLayer.FEET, vertical = false)
            }
            if (layers.contains(TrapLayer.FLOOR)) {
                trapPositions[blockPos.below()] = TrapLayer.FLOOR
            }
            if (layers.contains(TrapLayer.CEILING)) {
                trapPositions[blockPos.above(2)] = TrapLayer.CEILING
            }
        }

        if (bodyY == y && layers.contains(TrapLayer.BODY)) {
            extendTrapAroundPos(blockPos, origin, TrapLayer.BODY, vertical = false)
        }
    }

    private fun extendLayers(spec: TrapSpec, origin: Set<BlockPos>, feetY: Int, bodyY: Int) {
        for (blockPos in trapPositions.keys.toList()) {
            val y = blockPos.y
            if (feetY == y && spec.extendFeet) {
                extendTrapAroundPos(blockPos, origin, TrapLayer.EXTEND_FEET, vertical = true)
            } else if (bodyY == y && spec.extendBody) {
                extendTrapAroundPos(blockPos, origin, TrapLayer.EXTEND_BODY, vertical = true)
            }
        }
    }

    private fun extendTrapAroundPos(pos: BlockPos, origin: Set<BlockPos>, trapLayer: TrapLayer, vertical: Boolean) {
        for (direction in Direction.entries) {
            if (!vertical && direction.axis.isVertical) continue
            val extend = pos.relative(direction)
            if (extend in origin) continue
            trapPositions[extend] = trapLayer
        }
    }

    fun getTrapPositions(): MutableSet<BlockPos> = trapPositions.keys

    fun contains(pos: BlockPos): Boolean = trapPositions.containsKey(pos)
}
