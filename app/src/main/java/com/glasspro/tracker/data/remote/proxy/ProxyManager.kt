package com.glasspro.tracker.data.remote.proxy

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ProxyEndpoint(
    val url: String,
    val isWs: Boolean,
    val latencyMs: Long = -1L,
    val isWorking: Boolean = false
)

data class ProxyStatusInfo(
    val activeBinanceUrl: String = "https://fapi.binance.com",
    val binanceLatencyMs: Long = 0L,
    val activeBybitUrl: String = "https://api.bybit.com",
    val bybitLatencyMs: Long = 0L,
    val activeOkxUrl: String = "https://www.okx.com",
    val okxLatencyMs: Long = 0L,
    val isFallbackActive: Boolean = false
)

/**
 * High-Availability Multi-Proxy Engine.
 * Tests multiple mirror endpoints for Binance, Bybit, OKX, Gate, and Bitget,
 * measures response latency, and routes requests to the fastest working proxy.
 */
class ProxyManager {

    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow(ProxyStatusInfo())
    val status: StateFlow<ProxyStatusInfo> = _status.asStateFlow()

    private val binanceRestMirrors = listOf(
        "https://fapi.binance.com",
        "https://fapi.binance.vision",
        "https://fapi.binance.org"
    )

    private val binanceWsMirrors = listOf(
        "wss://fstream.binance.com/ws/!forceOrder@arr",
        "wss://stream.binancefuture.com/ws/!forceOrder@arr",
        "wss://fstream.binance.org/ws/!forceOrder@arr"
    )

    private val bybitRestMirrors = listOf(
        "https://api.bybit.com",
        "https://api.bytick.com"
    )

    private val okxRestMirrors = listOf(
        "https://www.okx.com",
        "https://aws.okx.com"
    )

    private val activeRestUrls = ConcurrentHashMap<String, String>()
    private val activeWsUrls = ConcurrentHashMap<String, String>()

    init {
        activeRestUrls["Binance"] = binanceRestMirrors[0]
        activeRestUrls["Bybit"] = bybitRestMirrors[0]
        activeRestUrls["OKX"] = okxRestMirrors[0]

        activeWsUrls["Binance"] = binanceWsMirrors[0]
    }

    suspend fun testAllProxiesAndSelectFastest() = withContext(Dispatchers.IO) {
        Log.i(TAG, "⚡ Starting Multi-Proxy Latency & Speed Benchmark...")

        // 1. Test Binance REST Mirrors
        var bestBinanceUrl = binanceRestMirrors[0]
        var bestBinanceLatency = 9999L

        for (url in binanceRestMirrors) {
            val latency = measureRestLatency("$url/fapi/v1/ping")
            if (latency in 1..<bestBinanceLatency) {
                bestBinanceLatency = latency
                bestBinanceUrl = url
            }
        }

        if (bestBinanceLatency < 9999L) {
            activeRestUrls["Binance"] = bestBinanceUrl
            Log.i(TAG, "Selected Binance Proxy: $bestBinanceUrl (${bestBinanceLatency}ms)")
        }

        // 2. Test Bybit REST Mirrors
        var bestBybitUrl = bybitRestMirrors[0]
        var bestBybitLatency = 9999L

        for (url in bybitRestMirrors) {
            val latency = measureRestLatency("$url/v5/market/time")
            if (latency in 1..<bestBybitLatency) {
                bestBybitLatency = latency
                bestBybitUrl = url
            }
        }

        if (bestBybitLatency < 9999L) {
            activeRestUrls["Bybit"] = bestBybitUrl
            Log.i(TAG, "Selected Bybit Proxy: $bestBybitUrl (${bestBybitLatency}ms)")
        }

        // 3. Test OKX REST Mirrors
        var bestOkxUrl = okxRestMirrors[0]
        var bestOkxLatency = 9999L

        for (url in okxRestMirrors) {
            val latency = measureRestLatency("$url/api/v5/public/time")
            if (latency in 1..<bestOkxLatency) {
                bestOkxLatency = latency
                bestOkxUrl = url
            }
        }

        if (bestOkxLatency < 9999L) {
            activeRestUrls["OKX"] = bestOkxUrl
            Log.i(TAG, "Selected OKX Proxy: $bestOkxUrl (${bestOkxLatency}ms)")
        }

        _status.value = ProxyStatusInfo(
            activeBinanceUrl = bestBinanceUrl,
            binanceLatencyMs = if (bestBinanceLatency < 9999L) bestBinanceLatency else 0L,
            activeBybitUrl = bestBybitUrl,
            bybitLatencyMs = if (bestBybitLatency < 9999L) bestBybitLatency else 0L,
            activeOkxUrl = bestOkxUrl,
            okxLatencyMs = if (bestOkxLatency < 9999L) bestOkxLatency else 0L,
            isFallbackActive = false
        )
    }

    private fun measureRestLatency(pingUrl: String): Long {
        val start = System.currentTimeMillis()
        return try {
            val request = Request.Builder()
                .url(pingUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .get()
                .build()

            pingClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    System.currentTimeMillis() - start
                } else {
                    -1L
                }
            }
        } catch (_: Exception) {
            -1L
        }
    }

    fun getRestBaseUrl(exchange: String): String {
        return activeRestUrls[exchange] ?: when (exchange) {
            "Binance" -> binanceRestMirrors[0]
            "Bybit" -> bybitRestMirrors[0]
            "OKX" -> okxRestMirrors[0]
            else -> ""
        }
    }

    fun rotateRestBaseUrl(exchange: String): String {
        val mirrors = when (exchange) {
            "Binance" -> binanceRestMirrors
            "Bybit" -> bybitRestMirrors
            "OKX" -> okxRestMirrors
            else -> return ""
        }

        val current = activeRestUrls[exchange] ?: mirrors[0]
        val currentIndex = mirrors.indexOf(current)
        val nextIndex = (currentIndex + 1) % mirrors.size
        val nextUrl = mirrors[nextIndex]

        activeRestUrls[exchange] = nextUrl
        Log.w(TAG, "Rotated $exchange proxy to $nextUrl")
        return nextUrl
    }

    fun getWsUrl(exchange: String): String {
        return activeWsUrls[exchange] ?: when (exchange) {
            "Binance" -> binanceWsMirrors[0]
            else -> ""
        }
    }

    fun rotateWsUrl(exchange: String): String {
        val mirrors = when (exchange) {
            "Binance" -> binanceWsMirrors
            else -> return ""
        }

        val current = activeWsUrls[exchange] ?: mirrors[0]
        val currentIndex = mirrors.indexOf(current)
        val nextIndex = (currentIndex + 1) % mirrors.size
        val nextUrl = mirrors[nextIndex]

        activeWsUrls[exchange] = nextUrl
        Log.w(TAG, "Rotated $exchange WebSocket to $nextUrl")
        return nextUrl
    }

    fun setFallbackActive(active: Boolean) {
        _status.value = _status.value.copy(isFallbackActive = active)
    }

    companion object {
        private const val TAG = "ProxyManager"
        val instance = ProxyManager()
    }
}
