package anpilot.client.renderer.rotation

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2

class MovementCorrection {
    
    

    fun correctMovement(deltaYaw: Float, forward: Float, sideways: Float): Vec2 {
        val delta = (deltaYaw * Mth.DEG_TO_RAD).toDouble()
        val cos = Math.cos(delta)
        val sin = Math.sin(delta)
        var f = (forward * cos + sideways * sin).toFloat()
        var g = (sideways * cos - forward * sin).toFloat()
        
        return Vec2(g, f)
    }
}
