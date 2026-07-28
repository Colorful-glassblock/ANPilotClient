package anpilot.client.features.event

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ANEventHandler(val priority: Int = ANEventPriority.MEDIUM)
