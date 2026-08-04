package com.glasspro.tracker.data.local

import android.content.Context

class UserPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("glass_settings", Context.MODE_PRIVATE)

    fun getMinThreshold(): Double = prefs.getFloat("min_threshold", 10000.0f).toDouble()

    fun setMinThreshold(value: Double) {
        prefs.edit().putFloat("min_threshold", value.toFloat()).apply()
    }

    fun getExcludeBtcEth(): Boolean = prefs.getBoolean("exclude_btc_eth", false)

    fun setExcludeBtcEth(value: Boolean) {
        prefs.edit().putBoolean("exclude_btc_eth", value).apply()
    }
}
