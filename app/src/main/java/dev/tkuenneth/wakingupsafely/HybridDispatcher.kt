package dev.tkuenneth.wakingupsafely

// Slide 22: "Routing by policy"
class HybridDispatcher(
    private val listenerPath: AlarmDispatcher,
    private val broadcastPath: AlarmDispatcher,
) : AlarmDispatcher {
    override fun schedule(
        tag: String, at: Long,
        policy: Delivery, work: () -> Unit,
    ) {
        cancel(tag)   // the tag may sit on the other path
        when (policy) {
            Delivery.WHILE_RUNNING ->
                listenerPath.schedule(tag, at, policy, work)
            Delivery.ALWAYS ->
                broadcastPath.schedule(tag, at, policy, work)
        }
    }

    override fun cancel(tag: String) {
        listenerPath.cancel(tag)
        broadcastPath.cancel(tag)
    }
}
