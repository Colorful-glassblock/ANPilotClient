package anpilot.client.features.event

import anpilot.client.api.event.ANCancellableEvent

open class ANCancellable : ANCancellableEvent {
    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}
