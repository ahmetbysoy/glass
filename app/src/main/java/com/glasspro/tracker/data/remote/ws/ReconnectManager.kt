package com.glasspro.tracker.data.remote.ws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ReconnectManager(private val scope: CoroutineScope, private val onReconnect: () -> Unit) {
    fun scheduleReconnect(delayMs: Long = 3000L) {
        scope.launch {
            delay(delayMs)
            if (isActive) onReconnect()
        }
    }
}
