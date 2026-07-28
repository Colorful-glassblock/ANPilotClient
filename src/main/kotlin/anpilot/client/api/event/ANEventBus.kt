package anpilot.client.api.event

interface ANEventBus {
    fun isListening(eventClass: Class<*>): Boolean

    fun <T : Any> post(event: T): T

    fun subscribe(listener: Any)

    fun subscribe(listenerClass: Class<*>)

    fun unsubscribe(listener: Any)

    fun unsubscribe(listenerClass: Class<*>)
}
