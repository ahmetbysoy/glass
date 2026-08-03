package com.example.data.engine

import com.example.data.model.LiquidationAnalysis
import com.example.data.model.LiquidationEvent
import com.example.data.model.LiquidationSide
import com.example.data.model.PredictionDirection
import com.example.data.model.PredictionStatus
import com.example.data.remote.RawMarketData
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LiquidationAnalysisEngine {

    fun analyze(
        event: LiquidationEvent,
        marketData: RawMarketData,
        rollingShortVol3m: Double,
        rollingLongVol3m: Double,
        rollingShortCount3m: Int,
        rollingLongCount3m: Int,
        geminiApiKey: String? = null
    ): LiquidationAnalysis {
        val sym = event.symbol
        val price = marketData.price ?: event.price
        val isCascade = rollingShortCount3m > 1 || event.volUsd >= 10_000.0

        val reasons = mutableListOf<String>()

        // 1. Liquidation Cascade Factor (Weight 0.30)
        val totalLiq3m = rollingShortVol3m + rollingLongVol3m
        val shortRatio = if (totalLiq3m > 0) rollingShortVol3m / totalLiq3m else 1.0
        val rawLiqScore = ((shortRatio - 0.5) * 2.0).coerceIn(-1.0, 1.0)

        // OI Relative scale check
        val oiUsd = marketData.oiUsd ?: 10_000_000.0
        val relImpactPct = if (oiUsd > 0) (event.volUsd / oiUsd) * 100.0 else 0.1
        val liqScore = if (relImpactPct < 0.05) rawLiqScore * 0.75 else rawLiqScore

        if (totalLiq3m > 0) {
            val cascadeNote = if (isCascade) " (kaskad: birden çok kısa tasfiye)" else ""
            val formattedVol = String.format("%,.0f", rollingShortVol3m)
            val pctStr = String.format("%.0f", shortRatio * 100)
            reasons.add("📈 Kısa tasfiye dalgası: son 3dk'da $rollingShortCount3m kısa / $rollingLongCount3m uzun tasfiye, kısa taraf $$formattedVol (%$pctStr)$cascadeNote. Tasfiye motoru alış yaptığı için yukarı baskı oluşturur.")
        } else {
            val formattedVol = String.format("%,.0f", event.volUsd)
            reasons.appendIfUnique("📈 Anlık kısa tasfiye: $$formattedVol tutarında short pozisyon tasfiye edildi. Tasfiye alımı yukarı ivme yaratır.")
        }

        // 2. Momentum Factor (Weight 0.22)
        var momentumScore = 0.0
        val candles = marketData.candles1m
        if (candles.size >= 5) {
            val lastClose = candles.last().close
            val prevClose = candles[candles.size - 5].close
            val pctChg = if (prevClose > 0) ((lastClose - prevClose) / prevClose) * 100.0 else 0.0
            momentumScore = (pctChg / 0.5).coerceIn(-1.0, 1.0)

            val arrow = if (pctChg >= 0) "▲" else "▼"
            val isGreenLast = candles.last().close >= candles.last().open
            val candleColor = if (isGreenLast) "yeşil" else "kırmızı"
            val pctFormatted = String.format("%+.2f", pctChg)
            reasons.add("📊 Momentum: son 5 mumda %$pctFormatted ($arrow); son 1m mum $candleColor")

            // Volume surge check
            val avgVol = candles.takeLast(5).map { it.volume }.average()
            val lastVol = candles.last().volume
            if (avgVol > 0 && lastVol / avgVol >= 1.5) {
                val surgeMult = String.format("%.1f", lastVol / avgVol)
                reasons.add("⚡ Hacim patlaması: son 1m mum hacmi ortalamanın ${surgeMult}x'i")
            }
        } else {
            reasons.add("📊 Momentum: yetersiz mum verisi, nötr alındı")
        }

        // 3. Aggressor Trade Flow Factor (Weight 0.18)
        var flowScore = 0.0
        val trades = marketData.trades
        val totalTradeNotional = trades.sumOf { it.price * it.qty }
        if (trades.isNotEmpty() && totalTradeNotional >= 1000.0) {
            val buyVol = trades.filter { it.side.lowercase() == "buy" }.sumOf { it.price * it.qty }
            val buyRatio = buyVol / max(1.0, totalTradeNotional)
            flowScore = ((buyRatio - 0.5) * 2.0).coerceIn(-1.0, 1.0)

            val buyRatioPct = (buyRatio * 100).toInt()
            val buyFormatted = String.format("%,.0f", buyVol)
            val sellFormatted = String.format("%,.0f", totalTradeNotional - buyVol)
            reasons.add("💵 Agresif akış: son ${trades.size} trade'de alış oranı %$buyRatioPct (hacim: alış $$buyFormatted vs satış $$sellFormatted)")
        } else if (trades.isNotEmpty()) {
            flowScore *= 0.3
            reasons.add("⚠️ Düşük işlem hacmi: trade akış sinyali zayıf ağırlıklandırıldı")
        }

        // 4. Order Book Imbalance Factor (Weight 0.15)
        var orderbookScore = 0.0
        val bids = marketData.bids.take(10)
        val asks = marketData.asks.take(10)
        val bidNotional = bids.sumOf { it.price * it.qty }
        val askNotional = asks.sumOf { it.price * it.qty }
        val bookTotalNotional = bidNotional + askNotional

        if (bookTotalNotional >= 5000.0) {
            val ratio = bidNotional / max(1.0, askNotional)
            orderbookScore = ((ratio - 1.0) / 1.5).coerceIn(-1.0, 1.0)

            val bidFmt = String.format("%,.0f", bidNotional)
            val askFmt = String.format("%,.0f", askNotional)
            val ratioFmt = String.format("%.2f", ratio)
            reasons.add("📚 Emir defteri: ilk 10 kademede alış $$bidFmt vs satış $$askFmt (oran $ratioFmt)")
        } else if (bookTotalNotional > 0) {
            orderbookScore *= 0.25
            val bookFmt = String.format("%,.0f", bookTotalNotional)
            reasons.add("⚠️ Düşük likidite: emir defteri toplamı ~$bookFmt — defter sinyali zayıf alındı")
        }

        // 5. Funding Rate Squeeze Factor (Weight 0.10)
        var fundingScore = 0.0
        val frate = marketData.fundingRate
        if (frate != null) {
            fundingScore = (-frate / 0.0005).coerceIn(-1.0, 1.0)
            val fratePct = String.format("%.4f", frate * 100)
            if (frate < 0) {
                reasons.add("💰 Funding: %$fratePct — negatif (short'lar ödüyor) → squeeze yakıtı, yukarıyı destekler")
            } else {
                reasons.add("💰 Funding: %$fratePct — pozitif (long'lar ödüyor) → uzun yoğunluğu, geri çekilme riski")
            }
        }

        // 6. Regime & Overbought Extension Factor (Weight 0.05 + Regime Dampening)
        var extensionScore = 0.0
        var regimeMult = 1.0

        if (candles.size >= 8) {
            val window = candles.takeLast(10)
            val maxHigh = window.maxOf { it.high }
            val minLow = window.minOf { it.low }
            val rangePct = if (price > 0) ((maxHigh - minLow) / price) * 100.0 else 1.0

            // Regime dampening if range is very flat
            if (rangePct < 0.5) {
                regimeMult = 0.5
                val rangeFmt = String.format("%.2f", rangePct)
                reasons.add("🌫️ Sakin seyir: son 10 mumda fiyat aralığı %$rangeFmt — yön sinyalleri YATAY'a çekildi")
            }

            val posInRange = if (maxHigh > minLow) (price - minLow) / (maxHigh - minLow) else 0.5
            if (posInRange > 0.85) {
                extensionScore = -0.6
                val posPct = (posInRange * 100).toInt()
                reasons.add("⚠️ Aşırı uzama: fiyat son aralığın üst %$posPct'inde → geri çekilme riski")
            } else if (posInRange < 0.15) {
                extensionScore = +0.4
                reasons.add("🌊 Dip bölgesi: fiyat son aralığın alt %${(posInRange*100).toInt()}'sinde")
            }
        }

        // Calculate final weighted score
        val rawScore = (liqScore * 0.30 +
                momentumScore * 0.22 +
                flowScore * 0.18 +
                orderbookScore * 0.15 +
                fundingScore * 0.10 +
                extensionScore * 0.05) * regimeMult

        val finalScore = rawScore.coerceIn(-1.0, 1.0)

        val direction = when {
            finalScore > 0.22 -> PredictionDirection.YUKARI
            finalScore < -0.22 -> PredictionDirection.ASAGI
            else -> PredictionDirection.YATAY
        }

        val baseConfidence = 55 + (abs(finalScore) * 35).toInt()
        val confidence = min(92, max(50, baseConfidence))

        // Support & Resistance
        val windowCandles = if (candles.isNotEmpty()) candles.takeLast(10) else emptyList()
        val supp = if (windowCandles.isNotEmpty()) windowCandles.minOf { it.low } else price * 0.985
        val res = if (windowCandles.isNotEmpty()) windowCandles.maxOf { it.high } else price * 1.015

        return LiquidationAnalysis(
            id = UUID.randomUUID().toString(),
            liquidationId = event.id,
            symbol = sym,
            originalSymbol = event.originalSymbol,
            exchangeName = event.exchangeName,
            triggerPrice = event.price,
            triggerVolUsd = event.volUsd,
            isCascade = isCascade,
            direction = direction,
            confidence = confidence,
            score = finalScore,
            reasons = reasons,
            supportPrice = supp,
            resistancePrice = res,
            currentPriceAtAnalysis = price,
            status = PredictionStatus.PENDING,
            providerUsed = marketData.provider,
            cascadeShortVol3m = rollingShortVol3m,
            cascadeShortCount3m = rollingShortCount3m
        )
    }

    private fun MutableList<String>.appendIfUnique(msg: String) {
        if (!this.contains(msg)) {
            this.add(msg)
        }
    }
}
