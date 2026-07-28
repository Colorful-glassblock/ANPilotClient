package anpilot.client.bootstrap

object ANServiceRegistry {
    lateinit var runtime: ANClientRuntime
        private set

    val isInitialized: Boolean
        get() = ::runtime.isInitialized

    fun initialize(runtime: ANClientRuntime) {
        this.runtime = runtime
    }
}
