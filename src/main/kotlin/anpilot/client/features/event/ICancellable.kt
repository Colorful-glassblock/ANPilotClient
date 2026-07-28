package anpilot.client.features.event

interface ICancellable {
    fun setCancelled(cancelled: Boolean)

    fun cancel() = setCancelled(true)

    fun isCancelled(): Boolean
}
