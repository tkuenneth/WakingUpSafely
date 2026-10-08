package dev.tkuenneth.wakingupsafely

import java.util.concurrent.ConcurrentHashMap

// Slide 23: "After a cold start, work comes from a registry, filled by tag at app start"
object WorkRegistry {
    private val work = ConcurrentHashMap<String, () -> Unit>()

    fun register(tag: String, block: () -> Unit) {
        work[tag] = block
    }

    operator fun get(tag: String): (() -> Unit)? = work[tag]
}
