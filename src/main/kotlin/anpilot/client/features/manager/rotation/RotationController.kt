package anpilot.client.features.manager.rotation

class RotationController {
    private val requests = mutableListOf<RotationRequest>()
    val state = RotationState()

    fun requestRotation(request: RotationRequest) {
        requests.add(request)
    }

    fun getHighestPriorityRequest(): RotationRequest? {
        return requests.maxWithOrNull(
            compareBy<RotationRequest> { it.priority.ordinal }
                .thenBy { requests.indexOf(it) }
        )
    }

    fun clearRequests() {
        requests.clear()
    }
}
