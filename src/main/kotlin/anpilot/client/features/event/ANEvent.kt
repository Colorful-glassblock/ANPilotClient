package anpilot.client.features.event

import anpilot.client.api.event.ANCancellableEvent

open class ANEvent(
    var stage: Stage = Stage.Pre
) : ANCancellableEvent, ICancellable {
    private var cancelled = false

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }

    override fun isCancelled(): Boolean = cancelled

    override fun cancel() {
        setCancelled(true)
    }

    fun isPost(): Boolean = stage == Stage.Post

    fun isPre(): Boolean = stage == Stage.Pre

    enum class Stage {
        Pre,
        Post
    }
}
