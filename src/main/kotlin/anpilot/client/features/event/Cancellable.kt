package anpilot.client.features.event

open class Cancellable : ICancellable {
    private var cancelled = false

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }

    override fun isCancelled(): Boolean = cancelled

    override fun cancel() {
        setCancelled(true)
    }
}
