package com.glasspro.tracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "glass_settings")

class UserPreferences(private val context: Context) {
    val minThresholdFlow: Flow<Double> = context.dataStore.data.map { it[MIN_THRESHOLD] ?: 10000.0 }
    val excludeBtcEthFlow: Flow<Boolean> = context.dataStore.data.map { it[EXCLUDE_BTC_ETH] ?: false }

    suspend fun setMinThreshold(value: Double) {
        context.dataStore.edit { it[MIN_THRESHOLD] = value }
    }

    suspend fun setExcludeBtcEth(value: Boolean) {
        context.dataStore.edit { it[EXCLUDE_BTC_ETH] = value }
    }

    companion object {
        val MIN_THRESHOLD = androidx.datastore.preferences.core.doublePreferencesKey("min_threshold")
        val EXCLUDE_BTC_ETH = androidx.datastore.preferences.core.booleanPreferencesKey("exclude_btc_eth")
    }
}
