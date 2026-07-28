package anpilot.client.features.utility





class QueueAverage(private val capacity: Int, private val clearTime: Long = -1L) {
    private val buf = DoubleArray(capacity)
    private var idx = 0
    private var count = 0
    private var sum = 0.0
    private val lastAddTime = ANTimer()

    fun add(v: Double) {
        lastAddTime.reset()
        if (count < buf.size) {
            buf[idx] = v
            sum += v
            count++
        } else {
            sum -= buf[idx]
            buf[idx] = v
            sum += v
        }
        if (++idx == buf.size) idx = 0
    }

    fun average(): Double {
        if (clearTime != -1L && lastAddTime.passedMs(clearTime)) clear()
        return if (count == 0) 0.0 else sum / count
    }

    fun latest(): Double {
        if (count == 0) return 0.0
        if (clearTime != -1L && lastAddTime.passedMs(clearTime)) clear()
        val last = if (idx == 0) buf.size - 1 else idx - 1
        return buf[last]
    }

    fun size(): Int = count

    fun clear() {
        idx = 0
        count = 0
        sum = 0.0
        buf.fill(0.0)
    }
}