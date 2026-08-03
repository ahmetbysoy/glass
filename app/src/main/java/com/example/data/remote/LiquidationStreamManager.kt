package com.example.data.remote

import android.util.Log
import com.example.data.db.AnalysisDao
import com.example.data.db.AnalysisEntity
import com.example.data.db.Converters
import com.example.data.db.LiquidationDao
import com.example.data.db.LiquidationEntity
import com.example.data.engine.LiquidationAnalysisEngine
import com.example.data.model.LiquidationEvent
import com.example.data.model.LiquidationSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

class LiquidationStreamManager(
    private val liquidationDao: LiquidationDao,
    private val analysisDao: AnalysisDao,
    private val exchangeService: ExchangeDataService,
    private val analysisEngine: LiquidationAnalysisEngine,
    private val scope: CoroutineScope
) {

    // Configurable state
    var minUsdThreshold: Double = 5000.0
    var excludeBtcEth: Boolean = true
    var isLiveStreaming: Boolean = true
    var cooldownSec: Long = 60L

    private val _latestAlertEvent = MutableSharedFlow<Pair<LiquidationEvent, String>>(extraBufferCapacity = 10)
    val latestAlertEvent: SharedFlow<Pair<LiquidationEvent, String>> = _latestAlertEvent.asSharedFlow()

    // 3-minute rolling window tracking per symbol: Pair<timestamp, volUsd>
    private val shortRollMap = mutableMapOf<String, ArrayDeque<Pair<Long, Double>>>()
    private val longRollMap = mutableMapOf<String, ArrayDeque<Pair<Long, Double>>>()
    private val lastAnalyzedMap = mutableMapOf<String, Long>()

    private val converters = Converters()

    private val altcoinList = listOf(
        "HOME" to 0.0083,
        "KAITO" to 1.06,
        "1000RATS" to 0.053,
        "LIT" to 2.04,
        "BLESS" to 0.0185,
        "ICNT" to 0.155,
        "SKHYNIX" to 1125.0,
        "ZEC" to 478.0,
        "SOL" to 73.5,
        "WLD" to 0.315,
        "PUMP" to 0.0021,
        "KOMA" to 0.0165,
        "BEAT" to 3.45,
        "HOOD" to 88.2,
        "AEON" to 0.071,
        "GIGGLE" to 42.1
    )

    private val exchanges = listOf("Binance", "Bybit", "OKX", "Bitget")

    fun startStream() {
        scope.launch(Dispatchers.IO) {
            var counter = 0
            while (isActive) {
                if (isLiveStreaming) {
                    try {
                        counter++
                        val event = generateStreamEvent(counter)
                        processEvent(event)
                    } catch (e: Exception) {
                        Log.e("LiquidationStreamManager", "Error processing stream event: ${e.message}")
                    }
                }
                // Stream ticks every 1.5 - 3.5 seconds
                delay(1500L + Random.nextLong(2000L))
            }
        }
    }

    suspend fun triggerManualAnalysis(symbol: String): String = withContext(Dispatchers.IO) {
        val cleanSym = symbol.uppercase().trim()
        val price = exchangeService.fetchMarketData(cleanSym).price ?: 1.0
        val vol = minUsdThreshold * (1.2 + Random.nextDouble() * 2.0)

        val event = LiquidationEvent(
            id = UUID.randomUUID().toString(),
            symbol = cleanSym,
            originalSymbol = "${cleanSym}USDT",
            exchangeName = "OKX",
            price = price,
            side = LiquidationSide.KISA,
            volUsd = vol,
            timestamp = System.currentTimeMillis(),
            isAltcoin = true
        )

        processEvent(event, forceAnalyze = true)
        return@withContext event.id
    }

    private suspend fun processEvent(event: LiquidationEvent, forceAnalyze: Boolean = false) {
        // Save event to Room DB
        liquidationDao.insertLiquidation(
            LiquidationEntity(
                id = event.id,
                symbol = event.symbol,
                originalSymbol = event.originalSymbol,
                exchangeName = event.exchangeName,
                price = event.price,
                side = event.side.name,
                volUsd = event.volUsd,
                timestamp = event.timestamp,
                isAltcoin = event.isAltcoin
            )
        )

        // Update 3-min rolling window
        val now = System.currentTimeMillis()
        val cutoff3m = now - 180_000L

        val deque = if (event.side == LiquidationSide.KISA) {
            shortRollMap.getOrPut(event.symbol) { ArrayDeque() }
        } else {
            longRollMap.getOrPut(event.symbol) { ArrayDeque() }
        }

        deque.addLast(now to event.volUsd)

        // Cleanup old
        for (map in listOf(shortRollMap, longRollMap)) {
            val dq = map[event.symbol]
            while (dq != null && dq.isNotEmpty() && dq.first.first < cutoff3m) {
                dq.removeFirst()
            }
        }

        // Calculate rolling sums & counts
        val shortDeque = shortRollMap[event.symbol] ?: ArrayDeque()
        val longDeque = longRollMap[event.symbol] ?: ArrayDeque()

        val rollingShortVol3m = shortDeque.sumOf { it.second }
        val rollingShortCount3m = shortDeque.size
        val rollingLongVol3m = longDeque.sumOf { it.second }
        val rollingLongCount3m = longDeque.size

        // Check Trigger Condition:
        // Altcoin + Short Liquidation + (Single Event >= threshold OR 60s cascade sum >= threshold)
        val isExcluded = excludeBtcEth && (event.symbol == "BTC" || event.symbol == "ETH")
        val isShortLiq = event.side == LiquidationSide.KISA
        val isVolumeTrigger = event.volUsd >= minUsdThreshold || rollingShortVol3m >= minUsdThreshold

        val lastAnalyzed = lastAnalyzedMap[event.symbol] ?: 0L
        val isCooldownPassed = (now - lastAnalyzed) >= (cooldownSec * 1000L)

        if ((isShortLiq && !isExcluded && isVolumeTrigger && isCooldownPassed) || forceAnalyze) {
            lastAnalyzedMap[event.symbol] = now

            // Fetch live market data for prediction
            val marketData = exchangeService.fetchMarketData(event.symbol)

            // Run quantitative analysis engine
            val analysis = analysisEngine.analyze(
                event = event,
                marketData = marketData,
                rollingShortVol3m = rollingShortVol3m,
                rollingLongVol3m = rollingLongVol3m,
                rollingShortCount3m = rollingShortCount3m,
                rollingLongCount3m = rollingLongCount3m
            )

            // Save Analysis to Room DB
            analysisDao.insertAnalysis(
                AnalysisEntity(
                    id = analysis.id,
                    liquidationId = analysis.liquidationId,
                    symbol = analysis.symbol,
                    originalSymbol = analysis.originalSymbol,
                    exchangeName = analysis.exchangeName,
                    triggerPrice = analysis.triggerPrice,
                    triggerVolUsd = analysis.triggerVolUsd,
                    isCascade = analysis.isCascade,
                    direction = analysis.direction.name,
                    confidence = analysis.confidence,
                    score = analysis.score,
                    reasonsJson = converters.fromReasonsList(analysis.reasons),
                    supportPrice = analysis.supportPrice,
                    resistancePrice = analysis.resistancePrice,
                    currentPriceAtAnalysis = analysis.currentPriceAtAnalysis,
                    status = analysis.status.name,
                    actualPrice = null,
                    priceChangePct = null,
                    createdAt = analysis.createdAt,
                    targetVerifyAt = analysis.targetVerifyAt,
                    providerUsed = analysis.providerUsed,
                    cascadeShortVol3m = analysis.cascadeShortVol3m,
                    cascadeShortCount3m = analysis.cascadeShortCount3m
                )
            )

            // Emit live trigger popup
            _latestAlertEvent.emit(event to analysis.direction.symbol)
        }
    }

    private fun generateStreamEvent(tick: Int): LiquidationEvent {
        val (sym, basePx) = altcoinList[Random.nextInt(altcoinList.size)]
        val ex = exchanges[Random.nextInt(exchanges.size)]

        // 70% short liquidations (to mimic active short squeezes), 30% long
        val isShort = Random.nextDouble() < 0.70
        val side = if (isShort) LiquidationSide.KISA else LiquidationSide.UZUN

        // Generate occasional big short liquidation spikes above threshold
        val isBigSpike = isShort && (tick % 4 == 0 || Random.nextDouble() < 0.25)
        val volUsd = if (isBigSpike) {
            minUsdThreshold + Random.nextDouble() * (minUsdThreshold * 4.0)
        } else {
            500.0 + Random.nextDouble() * 4200.0
        }

        val priceFluctuation = (Random.nextDouble() - 0.49) * 0.008
        val px = basePx * (1.0 + priceFluctuation)

        return LiquidationEvent(
            id = UUID.randomUUID().toString(),
            symbol = sym,
            originalSymbol = "${sym}USDT",
            exchangeName = ex,
            price = px,
            side = side,
            volUsd = ((volUsd * 100.0).roundToInt() / 100.0),
            timestamp = System.currentTimeMillis(),
            isAltcoin = true
        )
    }
}
