package com.glasspro.tracker.core.math

import com.glasspro.tracker.core.model.Candle
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * John Ehlers N-Pole Recursive IIR Gaussian Filter.
 * Computes an N-pole Gaussian filter with period and poles N (1..4).
 */
object EhlersGaussianFilter {

    private fun binom(n: Int, k: Int): Double {
        if (k < 0 || k > n) return 0.0
        var r = 1.0
        for (i in 0 until k) {
            r = r * (n - i) / (i + 1)
        }
        return r
    }

    fun compute(bars: List<Candle>, period: Int = 14, poles: Int = 2): List<Double?> {
        val nPoles = maxOf(1, minOf(4, poles))
        val beta = (1.0 - cos(2.0 * Math.PI / period)) / (1.414.pow(2.0 / nPoles) - 1.0)
        val alpha = -beta + sqrt(beta * beta + 2.0 * beta)
        val out = MutableList<Double?>(bars.size) { null }

        val coeffs = DoubleArray(nPoles)
        for (k in 1..nPoles) {
            coeffs[k - 1] = binom(nPoles, k) * (-1.0).pow(k + 1) * (1.0 - alpha).pow(k)
        }
        val alphaN = alpha.pow(nPoles)

        for (i in bars.indices) {
            val price = bars[i].close
            var valY = alphaN * price
            for (k in 1..nPoles) {
                val idx = i - k
                if (idx >= 0 && out[idx] != null) {
                    valY += coeffs[k - 1] * out[idx]!!
                }
            }
            out[i] = valY
        }
        return out
    }
}
