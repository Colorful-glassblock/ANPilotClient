package anpilot.client.features.utility

import java.util.concurrent.CopyOnWriteArrayList




class PerSecond {
    private val counter = CopyOnWriteArrayList<Long>()

    fun count() {
        counter.add(System.currentTimeMillis() + 1000L)
    }

    fun getPerSecond(): Int {
        val time = System.currentTimeMillis()
        while (counter.isNotEmpty() && counter.first() < time) {
            counter.removeFirst()
        }
        return counter.size
    }
}
