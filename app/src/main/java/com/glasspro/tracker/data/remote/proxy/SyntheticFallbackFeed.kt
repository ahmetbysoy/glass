package com.glasspro.tracker.data.remote.proxy

import android.util.Log
import com.glasspro.tracker.core.model.LiquidationEvent
import com.glasspro.tracker.core.model.LiquidationSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * High-Reliability Fallback Stream Generator.
 * Guarantees that the UI is NEVER empty/blank under extreme network censorship,
 * ISP blocks, or offline conditions.
 */
class SyntheticFallbackFeed(
    private val scope: CoroutineScope,
    private val onEvent: (LiquidationEvent) -> Unit
) {
    private var job: Job? = null
    private val symbols = listOf("BTC", "ETH", "SOL", "PEPE", "SUI", "XRP", "DOGE", "AVAX")
    private val exchanges = listOf("Binance", "Bybit", "OKX", "Hyperliquid", "Bitget")

    fun start() {
        if (job?.isActive == true) return
        Log.i("SyntheticFeed", "⚡ Hybrid Fallback Stream Generator Started")
        ProxyManager.instance.setFallbackActive(true)

        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val symbol = symbols.random()
                val exchange = exchanges.random()
                val isShort = Random.nextBoolean()

                val basePrice = when (symbol) {
                    "BTC" -> 64250.0
                    "ETH" -> 3480.0
                    "SOL" -> 175.0
                    "PEPE" -> 0.0000112
                    else -> 1.50
                }

                val price = basePrice * (1 + (Random.nextDouble() * 0.008 - 0.004))
                val notionalUsd = if (Random.nextDouble() > 0.85) {
                    Random.nextDouble(100000.0, 850000.0) // Whale
                } else {
                    Random.nextDouble(2500.0, 45000.0) // Standard
                }

                val qty = notionalUsd / price
                val timeMs = System.currentTimeMillis()

                val event = LiquidationEvent(
                    id = "SYNTH:$timeMs:${Random.nextInt(1000, 9999)}",
                    exchange = exchange,
                    symbol = symbol,
                    side = if (isShort) LiquidationSide.SHORT else LiquidationSide.LONG,
                    price = price,
                    quantity = qty,
                    notionalUsd = notionalUsd,
                    timestampNs = timeMs * 1_000_000L,
                    sequence = null,
                    isSnapshot = false,
                    sourceChannel = "hybrid-fallback"
                )

                onEvent(event)
                delay(Random.nextLong(600, 1800)) // Every 0.6 - 1.8 seconds
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        ProxyManager.instance.setFallbackActive(false)
    }
}
