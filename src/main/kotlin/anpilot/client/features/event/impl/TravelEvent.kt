package anpilot.client.features.event.impl 


import anpilot.client.features.event.Cancellable
import net.minecraft.world.phys.Vec3

open class TravelEvent(var movementInput: Vec3) : Cancellable() {


    class Pre(movementInput: Vec3) : TravelEvent(movementInput)

    
    class Post(movementInput: Vec3) : TravelEvent(movementInput)
}