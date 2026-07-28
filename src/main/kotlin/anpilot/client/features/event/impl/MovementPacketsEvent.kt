package anpilot.client.features.event.impl


import anpilot.client.features.event.Cancellable
import net.minecraft.network.protocol.Packet


open class MovementPacketsEvent : Cancellable() {

    class Update : MovementPacketsEvent()
    class Send(var packet: Packet<*>) : MovementPacketsEvent()
}