package anpilot.client.features.utility





class Animation(
    initialState: Boolean = false,
    var length: Float = 250f,
    var easing: (Double) -> Double = { smoothStep(it) }
) {
    private var last: Long = 0L
    var state: Boolean = initialState
        set(value) {
            if (field == value) return
            last = if (!value) {
                (System.currentTimeMillis().toDouble() - ((1.0 - getFactor()) * length.toDouble())).toLong()
            } else {
                (System.currentTimeMillis().toDouble() - (getFactor() * length.toDouble())).toLong()
            }
            field = value
        }

    init {
        last = if (initialState) {
            System.currentTimeMillis()
        } else {
            (System.currentTimeMillis().toDouble() - length.toDouble()).toLong()
        }
    }

    fun getFactor(): Double = easing(getLinearFactor())

    fun getLinearFactor(): Double {
        val raw = if (state) {
            (System.currentTimeMillis() - last) / length.toDouble()
        } else {
            1.0 - (System.currentTimeMillis() - last) / length.toDouble()
        }
        return raw.coerceIn(0.0, 1.0)
    }

    fun isFinished(): Boolean = (!state && getFactor() == 0.0) || (state && getFactor() == 1.0)

    fun reset() {
        last = System.currentTimeMillis()
    }

    companion object {
        fun smoothStep(t: Double): Double = t * t * (3.0 - 2.0 * t)

        fun easeOutCubic(t: Double): Double {
            val reversed = 1.0 - t
            return 1.0 - reversed * reversed * reversed
        }

    }
}
