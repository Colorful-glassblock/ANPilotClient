package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class ANAttackBlockEvent(val blockPos: BlockPos, val direction: Direction) : Cancellable()
