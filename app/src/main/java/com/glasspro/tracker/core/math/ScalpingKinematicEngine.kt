package com.glasspro.tracker.core.math

import java.util.ArrayDeque

data class PricePoint(val timestampMs: Long, val price: Double)

data class KinematicResult(
    val velocity: Double,        // Speed dPrice / dt
    val acceleration: Double,    // Acceleration dSpeed
    val signal: String,          // BULLISH_ACCELERATION, BEARISH_COLLAPSE, NEUTRAL
    val pctChange60s: Double
)

/**
 * Kinematic Momentum Engine (Velocity & Acceleration Scalping Engine).
 * Tracks a 60-second rolling window per symbol and calculates price speed (velocity)
 * and acceleration to detect explosive momentum breaks before they happen.
 */
class ScalpingKinematicEngine(
    private val windowMs: Long = 60_000L,
    private val speedThreshold: Double = 0.0001,
    private val accThreshold: Double = 0.00005
) {
    private val history = ArrayDeque<PricePoint>()

    fun pushPrice(timestampMs: Long, price: Double): KinematicResult {
        history.addLast(PricePoint(timestampMs, price))
        while (history.isNotEmpty() && timestampMs - history.first.timestampMs > windowMs) {
            history.removeFirst()
        }

        if (history.size < 5) {
            return KinematicResult(0.0, 0.0, "NEUTRAL", 0.0)
        }

        val openPrice = history.first.price
        val currPrice = history.last.price
        val pctChange = if (openPrice > 0) ((currPrice - openPrice) / openPrice) * 100.0 else 0.0

        val list = history.toList().takeLast(6)
        val speeds = mutableListOf<Double>()
        for (i in 1 until list.size) {
            val dt = (list[i].timestampMs - list[i - 1].timestampMs) / 1000.0
            if (dt > 0) {
                speeds.add((list[i].price - list[i - 1].price) / dt)
            }
        }

        if (speeds.size < 2) {
            return KinematicResult(0.0, 0.0, "NEUTRAL", pctChange)
        }

        val lastSpeed = speeds.last()
        val prevSpeed = speeds[speeds.size - 2]
        val acc = lastSpeed - prevSpeed

        val signal = when {
            acc > accThreshold && lastSpeed > speedThreshold -> "BULLISH_ACCELERATION"
            acc < -accThreshold && lastSpeed < -speedThreshold -> "BEARISH_COLLAPSE"
            else -> "NEUTRAL"
        }

        return KinematicResult(
            velocity = lastSpeed,
            acceleration = acc,
            signal = signal,
            pctChange60s = pctChange
        )
    }
}
