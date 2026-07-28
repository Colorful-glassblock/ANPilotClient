package anpilot.client.features.event

import anpilot.client.api.event.ANCancellableEvent
import anpilot.client.api.event.ANEventBus
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ANEventBusImpl : ANEventBus {
    private val listenerCache = ConcurrentHashMap<Any, List<Listener>>()
    private val staticListenerCache = ConcurrentHashMap<Class<*>, List<Listener>>()
    private val listenerMap = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<Listener>>()

    override fun isListening(eventClass: Class<*>): Boolean = !listenerMap[eventClass].isNullOrEmpty()

    override fun <T : Any> post(event: T): T {
        if (event is ANCancellableEvent) {
            event.setCancelled(false)
        }

        listenerMap[event.javaClass]?.forEach { listener ->
            listener.call(event)
            if (event is ANCancellableEvent && event.isCancelled()) {
                return event
            }
        }

        return event
    }

    override fun subscribe(listener: Any) {
        subscribe(getListeners(listener.javaClass, listener), false)
    }

    override fun subscribe(listenerClass: Class<*>) {
        subscribe(getListeners(listenerClass, null), true)
    }

    override fun unsubscribe(listener: Any) {
        unsubscribe(getListeners(listener.javaClass, listener), false)
    }

    override fun unsubscribe(listenerClass: Class<*>) {
        unsubscribe(getListeners(listenerClass, null), true)
    }

    private fun subscribe(listeners: List<Listener>, onlyStatic: Boolean) {
        listeners.forEach { listener ->
            if (!onlyStatic || listener.isStatic) {
                val targetListeners = listenerMap.computeIfAbsent(listener.target) { CopyOnWriteArrayList() }
                val insertIndex = targetListeners.indexOfFirst { listener.priority > it.priority }
                if (insertIndex == -1) {
                    targetListeners.add(listener)
                } else {
                    targetListeners.add(insertIndex, listener)
                }
            }
        }
    }

    private fun unsubscribe(listeners: List<Listener>, onlyStatic: Boolean) {
        listeners.forEach { listener ->
            if (!onlyStatic || listener.isStatic) {
                listenerMap[listener.target]?.remove(listener)
            }
        }
    }

    private fun getListeners(listenerClass: Class<*>, instance: Any?): List<Listener> {
        if (instance == null) {
            return staticListenerCache.computeIfAbsent(listenerClass) { collectListeners(listenerClass, null) }
        }

        listenerCache[instance]?.let { return it }
        return collectListeners(listenerClass, instance).also { listenerCache[instance] = it }
    }

    private fun collectListeners(listenerClass: Class<*>, instance: Any?): List<Listener> {
        val listeners = mutableListOf<Listener>()
        var current: Class<*>? = listenerClass
        while (current != null) {
            current.declaredMethods
                .filter(::isValid)
                .mapTo(listeners) { method -> Listener(instance, method) }
            current = current.superclass
        }
        return listeners
    }

    private fun isValid(method: Method): Boolean {
        return method.isAnnotationPresent(ANEventHandler::class.java) &&
            method.returnType == Void.TYPE &&
            method.parameterCount == 1 &&
            !method.parameterTypes[0].isPrimitive
    }

    private class Listener(
        private val instance: Any?,
        private val method: Method
    ) {
        val target: Class<*> = method.parameterTypes[0]
        val isStatic: Boolean = Modifier.isStatic(method.modifiers)
        val priority: Int = method.getAnnotation(ANEventHandler::class.java).priority

        init {
            method.isAccessible = true
        }

        fun call(event: Any) {
            method.invoke(instance, event)
        }

        override fun equals(other: Any?): Boolean {
            return other is Listener && other.instance === instance && other.method == method
        }

        override fun hashCode(): Int = 31 * System.identityHashCode(instance) + method.hashCode()
    }
}
