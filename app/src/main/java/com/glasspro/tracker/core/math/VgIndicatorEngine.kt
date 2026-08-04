package com.glasspro.tracker.core.math

import com.glasspro.tracker.core.model.Candle

data class VgPreset(
    val vwmaPeriod: Int,
    val gaussBandwidth: Double,
    val strongOn: Int,
    val strongOff: Int,
    val rsi45Only: Boolean,
    val alarm: Boolean,
    val lowEdge: Boolean
)

data class VgRsiBand(
    val bonus: Double,
    val label: String
)

data class VgSetupResult(
    val finalScore: Int,
    val badgeText: String,   // GÜÇLÜ SHORT 🪤, GÜÇLÜ SHORT, ZAYIF SHORT, BEKLE
    val badgeClass: String,  // assist, strong, weak, wait
    val isBullTrap: Boolean,
    val isRsiVeto: Boolean,
    val rsiLabel: String,
    val structuralScore: Double,
    val distPct: Double
)

/**
 * Quantitative VG Terminal Indicator Engine (v6 FADE Fit Engine).
 * Ports true Gaussian Kernel Smoother, Volume-Weighted Moving Average (VWMA),
 * Wilder RSI, Bull-Trap Assist (+10), and Hysteresis Signal Logic from PineScript / VG Terminal.
 */
object VgIndicatorEngine {

    val TF_PRESETS = mapOf(
        "1m" to VgPreset(21, 16.0, 76, 72, rsi45Only = true, alarm = false, lowEdge = true),
        "3m" to VgPreset(21, 16.0, 60, 56, rsi45Only = true, alarm = false, lowEdge = true),
        "5m" to VgPreset(21, 16.0, 76, 72, rsi45Only = true, alarm = false, lowEdge = true),
        "15m" to VgPreset(34, 8.0, 70, 66, rsi45Only = false, alarm = true, lowEdge = false),
        "1h" to VgPreset(34, 8.0, 70, 66, rsi45Only = false, alarm = false, lowEdge = false),
        "4h" to VgPreset(34, 8.0, 76, 72, rsi45Only = false, alarm = false, lowEdge = false)
    )

    fun computeVWMA(bars: List<Candle>, period: Int): List<Double?> {
        val out = MutableList<Double?>(bars.size) { null }
        var sumPV = 0.0
        var sumV = 0.0
        val pv = DoubleArray(bars.size)
        for (i in bars.indices) {
            pv[i] = bars[i].close * bars[i].volume
            sumPV += pv[i]
            sumV += bars[i].volume
            if (i >= period) {
                sumPV -= pv[i - period]
                sumV -= bars[i - period].volume
            }
            if (i >= period - 1 && sumV > 0.0) {
                out[i] = sumPV / sumV
            }
        }
        return out
    }

    fun computeGaussian(
        bars: List<Candle>,
        bandwidth: Double,
        srcOpt: String = "close"
    ): List<Double?> {
        val sigmaSq = maxOf(1e-9, bandwidth * bandwidth)
        val maxK = minOf(499, maxOf(1, Math.ceil(bandwidth * 4.8).toInt()))
        val out = MutableList<Double?>(bars.size) { null }
        if (bars.size <= maxK + 1) return out

        val srcArr = DoubleArray(bars.size) { i ->
            when (srcOpt) {
                "hl2" -> (bars[i].high + bars[i].low) / 2.0
                "hlc3" -> (bars[i].high + bars[i].low + bars[i].close) / 3.0
                "ohlc4" -> (bars[i].open + bars[i].high + bars[i].low + bars[i].close) / 4.0
                else -> bars[i].close
            }
        }

        val weights = DoubleArray(maxK + 1)
        var sumw = 0.0
        for (k in 0..maxK) {
            weights[k] = Math.exp(-(k * k).toDouble() / (2 * sigmaSq))
            sumw += weights[k]
        }

        for (i in (maxK + 1) until bars.size) {
            var sum = 0.0
            for (k in 0..maxK) {
                sum += weights[k] * srcArr[i - 1 - k]
            }
            out[i] = sum / sumw
        }
        return out
    }

    fun computeRSI(bars: List<Candle>, period: Int = 14): List<Double?> {
        val out = MutableList<Double?>(bars.size) { null }
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1 until bars.size) {
            val chg = bars[i].close - bars[i - 1].close
            val gain = if (chg > 0) chg else 0.0
            val loss = if (chg < 0) -chg else 0.0
            if (i <= period) {
                avgGain += gain
                avgLoss += loss
                if (i == period) {
                    avgGain /= period
                    avgLoss /= period
                    out[i] = if (avgLoss == 0.0) 100.0 else (100.0 - (100.0 / (1.0 + avgGain / avgLoss)))
                }
            } else {
                avgGain = (avgGain * (period - 1) + gain) / period
                avgLoss = (avgLoss * (period - 1) + loss) / period
                out[i] = if (avgLoss == 0.0) 100.0 else (100.0 - (100.0 / (1.0 + avgGain / avgLoss)))
            }
        }
        return out
    }

    fun rsiShortBand(rsi: Double?): VgRsiBand {
        if (rsi == null) return VgRsiBand(0.0, "n/a")
        return when {
            rsi < 30.0 -> VgRsiBand(8.0, "<30 dead-cat tepesi")
            rsi < 45.0 -> VgRsiBand(1.0, "30-45 zayıf")
            rsi < 55.0 -> VgRsiBand(4.0, "45-55 ideal dilim")
            rsi < 70.0 -> VgRsiBand(3.0, "55-70")
            else -> VgRsiBand(3.0, ">70 karışık")
        }
    }

    fun computeShortSetup(
        candles: List<Candle>,
        timeframe: String = "15m"
    ): VgSetupResult? {
        val preset = TF_PRESETS[timeframe] ?: TF_PRESETS["15m"]!!
        if (candles.size < 40) return null

        val vwmaSeries = computeVWMA(candles, preset.vwmaPeriod)
        val gaussSeries = computeGaussian(candles, preset.gaussBandwidth)
        val rsiSeries = computeRSI(candles, 14)

        val idx = candles.size - 1
        val price = candles[idx].close
        val vwma = vwmaSeries[idx] ?: return null
        val gauss = gaussSeries[idx] ?: return null
        val prevGauss = gaussSeries.getOrNull(idx - 1) ?: gauss
        val rsi = rsiSeries[idx]

        val distPct = (price - gauss) / gauss * 100.0
        val slopeUp = gauss > prevGauss

        // Search for bars since last Golden Cross (vwma > gauss)
        var barsSinceBuy: Int? = null
        for (k in 0 until minOf(30, idx)) {
            val i = idx - k
            val v = vwmaSeries.getOrNull(i)
            val g = gaussSeries.getOrNull(i)
            val prevV = vwmaSeries.getOrNull(i - 1)
            val prevG = gaussSeries.getOrNull(i - 1)
            if (v != null && g != null && prevV != null && prevG != null) {
                if (v > g && prevV <= prevG) {
                    barsSinceBuy = k
                    break
                }
            }
        }

        // Structural Score Formula
        var structural = 50.0
        structural += Math.max(-25.0, Math.min(25.0, distPct * 10.0))
        structural += if (slopeUp) 10.0 else -10.0
        structural += if (price >= vwma) 8.0 else -8.0
        structural = Math.max(0.0, Math.min(100.0, structural))

        val band = rsiShortBand(rsi)
        val baseScore = structural + band.bonus

        // Bull-Trap Bonus (+10 if Sell cross occurred 6-20 bars after Buy cross)
        val btActive = (barsSinceBuy != null && barsSinceBuy in 6..20)
        var finalScore = baseScore + (if (btActive) 10.0 else 0.0)
        finalScore = Math.max(0.0, Math.min(100.0, finalScore))

        // RSI Veto Rule
        val rsiVeto = preset.rsi45Only && rsi != null && rsi >= 45.0
        val isStrong = !rsiVeto && finalScore >= preset.strongOn
        val isAssist = isStrong && btActive && (baseScore < preset.strongOn)

        val badgeClass = when {
            isStrong && isAssist -> "assist"
            isStrong -> "strong"
            finalScore >= 50 -> "weak"
            else -> "wait"
        }

        val badgeText = when (badgeClass) {
            "assist" -> "GÜÇLÜ SHORT 🪤"
            "strong" -> "GÜÇLÜ SHORT"
            "weak" -> "ZAYIF SHORT"
            else -> "BEKLE"
        }

        return VgSetupResult(
            finalScore = Math.round(finalScore).toInt(),
            badgeText = badgeText,
            badgeClass = badgeClass,
            isBullTrap = btActive,
            isRsiVeto = rsiVeto,
            rsiLabel = band.label,
            structuralScore = structural,
            distPct = distPct
        )
    }
}
