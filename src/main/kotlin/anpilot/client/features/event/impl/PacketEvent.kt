package anpilot.client.features.event.impl

import anpilot.client.features.event.Cancellable
import net.minecraft.network.PacketListener
import net.minecraft.network.protocol.Packet

open class PacketEvent(var packet: Packet<*>) : Cancellable() {


    class Inbound(
        val packetListener: PacketListener,
        packet: Packet<*>,
        val isBundled: Boolean
    ) : PacketEvent(packet)


    class Outbound(packet: Packet<*>) : PacketEvent(packet)
    class InboundPost(packet: Packet<*>) : PacketEvent(packet)
    class OutboundPost(packet: Packet<*>) : PacketEvent(packet)

    class Receive(packet: Packet<*>) : PacketEvent(packet)
    class Send(packet: Packet<*>) : PacketEvent(packet)
    class Sent(packet: Packet<*>) : PacketEvent(packet)
}
