package anpilot.client.features.utility


class ANTimer() {
    private var lastTime: Long = 0

    init {
        reset()
    }

    fun passedS(seconds: Double): Boolean {
        return passedNanos((seconds * 1_000_000_000L).toLong())
    }


    fun passedMs(ms: Long): Boolean {
        return passedNanos(ms * 1_000_000L)
    }

    fun every(ms: Long): Boolean {
        if (passedMs(ms)) {
            reset()
            return true
        }
        return false
    }

    fun setMs(ms: Long) {
        this.lastTime = System.nanoTime() - ms * 1_000_000L
    }

    fun reset() {
        this.lastTime = System.nanoTime()
    }

    fun getMs(nanos: Long): Long {
        return nanos / 1_000_000L
    }

    val timeMs: Long
        get() = elapsedMs

    fun passedNanos(nanos: Long): Boolean {
        return System.nanoTime() - lastTime >= nanos
    }

    fun passedAndResetMs(ms: Long): Boolean {
        if (passedMs(ms)) {
            reset()
            return true
        }
        return false
    }


    val elapsedNanos: Long
        get() = System.nanoTime() - lastTime

    val elapsedMs: Long
        get() = elapsedNanos / 1_000_000L


    fun remainingMs(cooldownMs: Long): Long {
        val elapsed = elapsedMs
        return if (elapsed >= cooldownMs) 0L else cooldownMs - elapsed
    }
}