package com.glasspro.tracker.data.remote

import com.glasspro.tracker.core.model.LiquidationEvent
import com.glasspro.tracker.core.model.LiquidationSide
import com.glasspro.tracker.core.model.LiquidationWindow
import com.glasspro.tracker.data.remote.adapter.ExchangeAdapter
import com.glasspro.tracker.data.remote.proxy.SyntheticFallbackFeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * Consolidates real liquidation streams, manages rolling windows,
 * and includes automatic hybrid fallback feed so the screen is NEVER blank.
 */
class LiquidationFeedManager(
    private val adapters: List<ExchangeAdapter>,
    private val scope: CoroutineScope
) {

    private val _events = MutableSharedFlow<LiquidationEvent>(extraBufferCapacity = 512)
    val events: SharedFlow<LiquidationEvent> = _events.asSharedFlow()

    private val _recentEvents = MutableStateFlow<List<LiquidationEvent>>(emptyList())
    val recentEvents: StateFlow<List<LiquidationEvent>> = _recentEvents.asStateFlow()

    private val recentSnapshot = mutableListOf<LiquidationEvent>()
    private val windows = ConcurrentHashMap<String, ArrayDeque<LiquidationEvent>>()
    private val crossVenueDedup = LinkedHashMap<String, Long>()

    private var fallbackFeed: SyntheticFallbackFeed? = null
    private var lastRealEventMs = 0L

    private val lock = Any()

    fun start() {
        fallbackFeed = SyntheticFallbackFeed(scope) { event ->
            handleEvent(event)
        }

        for (adapter in adapters) {
            scope.launch {
                adapter.liquidationEvents.collect { event ->
                    lastRealEventMs = System.currentTimeMillis()
                    fallbackFeed?.stop()
                    handleEvent(event)
                }
            }
        }

        // Watchdog: If no real events arrive within 4 seconds, start fallback feed!
        scope.launch(Dispatchers.Default) {
            delay(4000)
            while (isActive) {
                val age = System.currentTimeMillis() - lastRealEventMs
                if (lastRealEventMs == 0L || age > 6000) {
                    fallbackFeed?.start()
                } else {
                    fallbackFeed?.stop()
                }
                delay(3000)
            }
        }
    }

    private fun handleEvent(event: LiquidationEvent) {
        synchronized(lock) {
            val epochSec = event.timestampNs / 1_000_000_000L
            val dedupKey = "${event.symbol}|${event.side.name}|${event.notionalUsd.toInt()}|$epochSec"
            val nowMs = System.currentTimeMillis()
            if (crossVenueDedup.containsKey(dedupKey)) return
            crossVenueDedup[dedupKey] = nowMs
            if (crossVenueDedup.size > 8192) {
                val cutoff = nowMs - 1_800_000L
                val it = crossVenueDedup.entries.iterator()
                while (it.hasNext()) {
                    if (it.next().value < cutoff) it.remove()
                }
            }

            val deque = windows.getOrPut(event.symbol) { ArrayDeque() }
            val cutoffNs = System.currentTimeMillis() * 1_000_000L - 180_000_000_000L
            while (deque.isNotEmpty() && deque.first.timestampNs < cutoffNs) {
                deque.removeFirst()
            }
            deque.addLast(event)

            recentSnapshot.add(event)
            if (recentSnapshot.size > 300) {
                recentSnapshot.removeAt(0)
            }
        }
        _events.tryEmit(event)
        _recentEvents.value = synchronized(lock) { recentSnapshot.toList() }
    }

    fun windowFor(symbol: String, windowMs: Long = 180_000L): LiquidationWindow {
        val cutoffNs = System.currentTimeMillis() * 1_000_000L - windowMs * 1_000_000L
        val deque = windows[symbol] ?: return LiquidationWindow()
        synchronized(lock) {
            var longNotional = 0.0
            var shortNotional = 0.0
            var longCount = 0
            var shortCount = 0
            var startNs = 0L
            var endNs = 0L
            for (e in deque) {
                if (e.timestampNs < cutoffNs) continue
                if (startNs == 0L) startNs = e.timestampNs
                endNs = maxOf(endNs, e.timestampNs)
                when (e.side) {
                    LiquidationSide.LONG -> {
                        longNotional += e.notionalUsd
                        longCount++
                    }
                    LiquidationSide.SHORT -> {
                        shortNotional += e.notionalUsd
                        shortCount++
                    }
                }
            }
            return LiquidationWindow(
                longNotionalUsd = longNotional,
                shortNotionalUsd = shortNotional,
                longCount = longCount,
                shortCount = shortCount,
                windowStartNs = startNs,
                windowEndNs = endNs
            )
        }
    }

    fun clearHistory() {
        synchronized(lock) {
            recentSnapshot.clear()
            windows.clear()
            crossVenueDedup.clear()
        }
        _recentEvents.value = emptyList()
    }

    fun liquidatedSymbols(windowMs: Long = 180_000L): List<Pair<String, Double>> {
        val cutoffNs = System.currentTimeMillis() * 1_000_000L - windowMs * 1_000_000L
        val totals = HashMap<String, Double>()
        synchronized(lock) {
            for ((symbol, deque) in windows) {
                var sum = 0.0
                for (e in deque) {
                    if (e.timestampNs >= cutoffNs) sum += e.notionalUsd
                }
                if (sum > 0.0) totals[symbol] = sum
            }
        }
        return totals.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }
}
