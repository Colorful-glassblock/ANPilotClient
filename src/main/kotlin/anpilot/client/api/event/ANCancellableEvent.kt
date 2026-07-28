package anpilot.client.api.event

interface ANCancellableEvent {
    fun isCancelled(): Boolean
    fun setCancelled(cancelled: Boolean)
}
